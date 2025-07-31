(ns ring.middleware.multipart-params.byte-array
  "A multipart storage engine for storing uploads as in-memory byte arrays."
  (:import [java.io InputStream OutputStream]
           [java.lang IndexOutOfBoundsException System]
           [org.apache.commons.io IOUtils]))

(def ^:private ^:const byte-array-type (type (byte-array 0)))

(defn- bounded-buffer
  "Creates a ByteArrayOutputStream-like buffer which enters a failure state
  if it is supplied with more than max-size bytes of data. It returns a
  vector containing the buffer, which subclasses OutputStream, and a function
  which collects the data in the buffer into a single byte array. If too much
  data is provided to the buffer, then the collect function will return
  :ring.middleware.multipart-params.byte-array/too-large."
  [max-size]
  ;; State consists of total length and a list of [arr i j n] vectors, where
  ;; arr is an array, [i, i + n) is a range in the array, and j is the
  ;; corresponding position in the stream.
  (let [state (atom [0 []])]
    [(proxy [OutputStream] []
       (write [v & more]
         (let [[arr i n] (if (= (type v) byte-array-type)
                           (if (empty? more)
                             [v 0 (count v)] ; write(byte[])
                             (vec (conj more v))) ; write(byte[], int, int)
                           [(byte-array [v]) 0 1])] ; write(int)
           (if (> n (- (count arr) i))
             (throw (IndexOutOfBoundsException.)))
           (swap! state (fn [[j arrs]]
                          (let [next (+ j n)]
                            (if (> next max-size)
                              nil
                              [next (conj arrs [arr i j n])])))))))
     (fn []
       (let [s @state]
         (if (nil? s)
           ::too-large
           (let [[len arrs] s
                 out (byte-array len)
                 idx (atom 0)]
             (run! (fn [[arr i j n]]
                     (System/arraycopy arr i out j n))
                   arrs)
             out))))]))

(defn byte-array-store
  "Returns a function that stores multipart file parameters as an array of
  bytes. Accepts the following options:

  :max-size - maximum size of files to accept, in bytes

  The multipart parameters will be stored as maps with the following keys:

  :filename     - the name of the uploaded file
  :content-type - the content type of the uploaded file
  :bytes        - an array of bytes containing the uploaded content"
  {:arglists '([] [options])}
  ([] (byte-array-store {}))
  ([options]
   (fn [item]
     (-> (select-keys item [:filename :content-type])
         (assoc :bytes (if (contains? options :max-size)
                         (let [[s f] (bounded-buffer (:max-size options))]
                           (.transferTo ^InputStream (:stream item) s)
                           (f))
                         (IOUtils/toByteArray
                          ^InputStream (:stream item))))))))

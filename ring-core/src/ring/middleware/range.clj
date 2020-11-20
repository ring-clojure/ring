(ns ring.middleware.range
  "Middleware that handles HTTP range requests"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ring.middleware.content-type :refer [content-type-response]]
            [ring.util.response :refer [status get-header header]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream File IOException InputStream RandomAccessFile]))

(defn- char-range
  [start end]
  (map char (range (int start) (inc (int end)))))

(def boundary-chars (vec (concat (char-range \A \Z) (char-range \a \z) (char-range \0 \9))))

(def ^:dynamic *boundary-generator* (fn []
                                      (->> (repeatedly #(rand-int (count boundary-chars)))
                                           (take 30)
                                           (map #(nth boundary-chars %))
                                           (apply str))))
(def range-unit "bytes")

(defn header-value->ranges
  [^String header-value]
  (when (.startsWith header-value (str range-unit "="))
    (let [rest (-> header-value
                   (subs (count range-unit))
                   ;; get rid of equal sign
                   (subs 1))
          split-groups (str/split rest #",")
          parsed-groups (map #(re-find #"^\s*(\d*)-(\d*)\s*$" %) split-groups)]
      (map (fn [[_ prefix suffix]]
             (let [prefix-int (if (empty? prefix) nil (Long/parseLong prefix))
                   suffix-int (if (empty? suffix) nil (Long/parseLong suffix))]
               (cond
                 (and (some? prefix-int) (some? suffix-int))
                 {:start prefix-int
                  :end suffix-int}

                 (some? prefix-int)
                 {:start prefix-int}

                 (some? suffix-int)
                 {:start (* -1 suffix-int)}

                 :else
                 {})))
           parsed-groups))))

(defn parse-ranges
  "If all byte ranges are valid and not overlapping, returns absolute byte offsets in order from smallest start/end
  offset to largest. Returns nil otherwise.
  Valid means the byte offsets in the ranges are within the body length."
  [header-value body-length]
  (let [ranges (header-value->ranges header-value)]
    (when (some? ranges)
      (let [last-byte-index (- body-length 1)
            transformed-ranges (map (fn [{:keys [start end]}]
                                      (cond
                                        (and (some? start) (some? end))
                                        [(and (<= 0 start last-byte-index)
                                              (<= 0 end last-byte-index))
                                         {:start start
                                          :end (inc end)}]

                                        (and (some? start) (not (some? end)))
                                        (let [actual-start (cond-> start
                                                                   (neg? start) (+ body-length))]
                                          [(and (<= 0 actual-start last-byte-index))
                                           {:start actual-start
                                            :end (inc last-byte-index)}])

                                        :else
                                        [false
                                         nil]))
                                    ranges)
            ranges-valid? (every? #(-> % (first) (true?)) transformed-ranges)]
        (when ranges-valid?
          (let [sorted-ranges (->> transformed-ranges
                                   (map second)
                                   (sort-by (juxt :start :end)))
                no-overlapping-ranges? (every? (fn [idx]
                                                 (let [prev-range (nth sorted-ranges idx)
                                                       next-range (nth sorted-ranges (inc idx))]
                                                   (<= (:end prev-range) (:start next-range))))
                                               (-> sorted-ranges (count) (- 1) (range)))]
            (when no-overlapping-ranges?
              sorted-ranges)))))))

(defn- is-bytes?
  "bytes? was added in Clojure 1.9 so adding an implementation here for backwards compatibility"
  [x]
  (if (nil? x)
    false
    (= (.getClass x) (Class/forName "[B"))))

(defn body-byte-range->string
  "Given a byte array or File body, gets the bytes specified between `start-offset` (inclusive)
  and `end-offset` (exclusive)."
  [body start-offset end-offset]
  {:post [(string? %)]}
  (cond
    (is-bytes? body)
    (->> (range start-offset end-offset)
         (map #(aget body %))
         (byte-array)
         (String.))

    (instance? File body)
    (with-open [random-access-file (RandomAccessFile. ^File body "r")]
      (let [byte-arr (byte-array (- end-offset start-offset))
            length (- end-offset start-offset)]
        (.seek random-access-file start-offset)
        (.read random-access-file byte-arr 0 length)
        (String. byte-arr)))

    :else
    (throw (IOException. (str "cannot get bytes from body type: " (type body))))))

(defn convert-body-to-bytearray-or-file!
  [body]
  {:post [(or (is-bytes? %) (instance? File %))]}
  (cond
    (string? body) (.getBytes body)
    (seq? body) (->> body (apply str) (.getBytes))
    (instance? File body) body
    (instance? InputStream body) (with-open [output-stream (ByteArrayOutputStream.)]
                                   (io/copy body output-stream)
                                   (.toByteArray output-stream))

    :else (throw (IOException. "unknown body type: " (type body)))))

(defn range-header-response
  "Returns the original response if no Range header present or if the header is invalid or
  if the existing body is not a String or a File, and 206 if the range header is
  successfully processed along with the requested bytes in the body.

  If multiple ranges are requested, the body is a multipart response."
  [response request]
  (let [response (header response "Accept-Ranges" "bytes")
        range-header (get-header request "Range")]
    (if (and (= 200 (:status response))
             (= :get (:request-method request))
             (some? range-header))
      (let [body (convert-body-to-bytearray-or-file! (:body response))
            body-length (cond
                          (is-bytes? body)
                          (alength body)

                          (instance? File body)
                          (.length body)

                          :else
                          (throw (IOException. "cannot get length for body type: " (type body))))
            ranges (parse-ranges range-header body-length)]
        (cond
          (= 1 (count ranges))
          (let [{:keys [start end]} (first ranges)]
            (-> response
                (status 206)
                (header "Content-Length" (- end start))
                (header "Content-Range" (format "bytes %d-%d/%d" start (dec end) body-length))
                (assoc :body (body-byte-range->string body start end))))

          (< 1 (count ranges))
          (let [response-with-content-type (content-type-response response request)
                content-type (get-header response-with-content-type "Content-Type")
                multipart-boundary-str (*boundary-generator*)
                body (->> ranges
                          (map (fn [{:keys [start end]}]
                                 (str/join "\r\n" [(format "Content-Type: %s" content-type)
                                                   (format "Content-Range: %d-%d/%d" start (dec end) body-length)
                                                   ""
                                                   (body-byte-range->string body start end)])))
                          (str/join (format "\r\n--%s\r\n" multipart-boundary-str)))
                body (str/join "\r\n" [(str "--" multipart-boundary-str)
                                       body
                                       (str "--" multipart-boundary-str "--")])]
            (-> response-with-content-type
                (status 206)
                (header "Content-Length" (-> body (.getBytes) (alength)))
                (header "Content-Type" (format "multipart/byteranges; boundary=%s" multipart-boundary-str))
                (assoc :body body)))
          :else
          response))
      response)))

(defn wrap-range-header
  "Middleware that attempts to fulfill the Range header in the request, if any.

  If the requested Range has valid byte offsets and no overlapping ranges, returns 206 with the requested bytes.
  Otherwise, returns the original response.

  A single range is returned directly as bytes in the body. Multiple ranges are returned as a multipart/byteranges
  response as per RFC7233."
  [handler]
  (fn
    ([request]
     (-> (handler request) (range-header-response request)))
    ([request respond raise]
     (handler request
              (fn [response] (respond (range-header-response response request)))
              raise))))
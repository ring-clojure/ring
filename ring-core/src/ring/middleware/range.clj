(ns ring.middleware.range
  "Middleware that handles HTTP range requests"
  (:require [clojure.string :as str]
            [ring.core.protocols :refer :all]
            [ring.middleware.content-type :refer [content-type-response]]
            [ring.util.response :refer [status get-header header]])
  (:import [java.io ByteArrayOutputStream File OutputStream]))

(def max-buffer-size-per-range-bytes (* 1024 1024))
(def max-num-ranges 10)

(def multipart-newline "\r\n")

(def optional-whitespace "[ \\t]*")
(def at-least-one-digit "\\d+")

(def first-byte-pos (str "(" at-least-one-digit ")"))
(def last-byte-pos (str "(" at-least-one-digit ")"))
(def byte-range-spec (str "(?:" first-byte-pos "\\-" (format "(?:%s)?" last-byte-pos) ")"))

(def suffix-length (str "(" at-least-one-digit ")"))
(def suffix-byte-range-spec (str "(?:\\-" suffix-length ")"))
(def byte-or-suffix-byte-range-spec (format "(?:%s|%s)" byte-range-spec suffix-byte-range-spec))
(def byte-range-set (str (format "(?:,%s)*" optional-whitespace)
                         byte-or-suffix-byte-range-spec
                         (format "(?:%s,(?:%s%s)?)*"
                                 optional-whitespace
                                 optional-whitespace
                                 byte-or-suffix-byte-range-spec)))
(def bytes-unit "bytes")
(def byte-ranges-specifier (str bytes-unit "=" byte-range-set))

(def byte-or-suffix-byte-range-spec-regex (re-pattern byte-or-suffix-byte-range-spec))
(def byte-ranges-specifier-regex (re-pattern byte-ranges-specifier))

(defprotocol IByteRange
  (start-byte [this total-body-length] "Returns the start byte offset, given the total body length")
  (end-byte [this total-body-length] "Returns the end byte offset, given the total body length")
  (length [this total-body-length] "Length of the range in bytes, given a total body length")
  (content-range [this total-body-length] "Value of the Content-Range header, given a total body length")
  (valid?
    [this]
    [this total-body-length]
    "Returns whether or not the range is valid. If the total body length is given, checks against that as well.")
  (to-closed-byte-range [this total-body-length] "If the total body length is known, every range can be converted to a closed range form. This fn does that.")
  (anticipated-number-of-bytes-to-write-to-stream [this bytes bytes-offset bytes-length num-bytes-read])
  (maybe-write-bytes-to-stream [this bytes bytes-offset bytes-length num-bytes-read output-stream]))

(defn- range->content-range-header-value
  [range total-body-length]
  (format "%s %d-%d/%s"
          bytes-unit
          (start-byte range total-body-length)
          (end-byte range total-body-length)
          total-body-length))

(defrecord ClosedByteRange [first-byte-pos last-byte-pos]
  IByteRange
  (start-byte [this total-body-length]
    first-byte-pos)
  (end-byte [this total-body-length]
    (min last-byte-pos (dec total-body-length)))
  (length [this total-body-length]
    (inc (- (end-byte this total-body-length) (start-byte this total-body-length))))
  (content-range [this total-body-length]
    (range->content-range-header-value this total-body-length))
  (valid? [this]
    (<= first-byte-pos last-byte-pos))
  (valid? [this total-body-length]
    (and (valid? this)
         (<= first-byte-pos total-body-length)))
  (to-closed-byte-range [this total-body-length]
    this)
  (anticipated-number-of-bytes-to-write-to-stream [this bytes bytes-offset bytes-length num-bytes-read]
    (let [bytes-absolute-start-offset num-bytes-read
          bytes-absolute-end-offset (dec (+ num-bytes-read bytes-length))

          range-start (max first-byte-pos bytes-absolute-start-offset)
          range-end (min last-byte-pos bytes-absolute-end-offset)]
      (max 0 (inc (- range-end range-start)))))
  (maybe-write-bytes-to-stream [this bytes bytes-offset bytes-length num-bytes-read output-stream]
    (let [bytes-absolute-start-offset num-bytes-read
          bytes-absolute-end-offset (dec (+ num-bytes-read bytes-length))

          range-start (max first-byte-pos bytes-absolute-start-offset)
          range-end (min last-byte-pos bytes-absolute-end-offset)
          should-write? (<= range-start range-end)]
      (when should-write?
        (let [bytes-absolute->relative-offset (+ bytes-absolute-start-offset
                                                 (- bytes-offset))]
          (.write output-stream
                  bytes
                  (+ range-start bytes-absolute->relative-offset)
                  (inc (- range-end range-start))))))))

(defrecord SuffixByteRange [suffix-length]
  IByteRange
  (start-byte [this total-body-length]
    (max 0 (- total-body-length suffix-length)))
  (end-byte [this total-body-length]
    (dec total-body-length))
  (length [this total-body-length]
    (inc (- (end-byte this total-body-length) (start-byte this total-body-length))))
  (content-range [this total-body-length]
    (range->content-range-header-value this total-body-length))
  (valid? [this]
    (< 0 suffix-length))
  (valid? [this total-body-length]
    (and (valid? this)
         (< 0 total-body-length)))
  (to-closed-byte-range [this total-body-length]
    (map->ClosedByteRange {:first-byte-pos (start-byte this total-body-length)
                           :last-byte-pos (end-byte this total-body-length)}))
  (anticipated-number-of-bytes-to-write-to-stream [this bytes bytes-offset bytes-length num-bytes-read]
    (min bytes-length (- suffix-length num-bytes-read)))
  (maybe-write-bytes-to-stream [this bytes bytes-offset bytes-length num-bytes-read output-stream]
    (let [bytes-relative-end-offset (+ bytes-offset bytes-length)
          length-to-take (min bytes-length max-buffer-size-per-range-bytes)
          bytes-relative-start-offset (- bytes-relative-end-offset length-to-take)]
      (.write output-stream
              bytes
              bytes-relative-start-offset
              length-to-take))))

(defrecord OpenEndedByteRange [first-byte-pos]
  IByteRange
  (start-byte [this total-body-length]
    first-byte-pos)
  (end-byte [this total-body-length]
    (dec total-body-length))
  (length [this total-body-length]
    (inc (- (end-byte this total-body-length) (start-byte this total-body-length))))
  (content-range [this total-body-length]
    (range->content-range-header-value this total-body-length))
  (valid? [this]
    true)
  (valid? [this total-body-length]
    (and (valid? this)
         (< first-byte-pos total-body-length)))
  (to-closed-byte-range [this total-body-length]
    (ClosedByteRange. (start-byte this total-body-length) (end-byte this total-body-length)))
  (anticipated-number-of-bytes-to-write-to-stream [this bytes bytes-offset bytes-length num-bytes-read]
    (let [bytes-start (max first-byte-pos num-bytes-read)
          bytes-end (dec (+ num-bytes-read bytes-length))]
      (max 0 (inc (- bytes-end bytes-start)))))
  (maybe-write-bytes-to-stream [this bytes bytes-offset bytes-length num-bytes-read output-stream]
    (let [bytes-start (max first-byte-pos num-bytes-read)
          bytes-end (dec (+ num-bytes-read bytes-length))
          bytes-absolute->relative-offset (- bytes-offset num-bytes-read)]
      (when (<= bytes-start bytes-end)
        (.write output-stream
                bytes
                (+ bytes-start bytes-absolute->relative-offset)
                (inc (- bytes-end bytes-start)))))))

(defn sort-byte-ranges
  [ranges]
  (sort-by (juxt :suffix-length :first-byte-pos :last-byte-pos) ranges))

(defn- maybe-parse-long
  [x]
  (some-> x (Long/parseLong)))

(defn- header-value->byte-ranges
  "If the header is valid, turns the header into a seq of ranges."
  [^String header-value]
  (when (and (some? header-value)
             (some? (re-matches byte-ranges-specifier-regex header-value)))
    (->> (re-seq byte-or-suffix-byte-range-spec-regex header-value)
         (map (fn [[_ first-byte-pos last-byte-pos suffix-byte-length]]
                (let [[first-byte-pos last-byte-pos suffix-length] (map maybe-parse-long [first-byte-pos last-byte-pos suffix-byte-length])]
                  (cond
                    (and (some? first-byte-pos) (some? last-byte-pos) (nil? suffix-length))
                    (map->ClosedByteRange {:first-byte-pos first-byte-pos :last-byte-pos last-byte-pos})

                    (and (some? first-byte-pos) (nil? last-byte-pos) (nil? suffix-length))
                    (map->OpenEndedByteRange {:first-byte-pos first-byte-pos})

                    (and (nil? first-byte-pos) (nil? last-byte-pos) (some? suffix-length))
                    (map->SuffixByteRange {:suffix-length suffix-length})))))
         (filter some?))))

(defn- convert-ranges-to-closed-byte
  [ranges total-body-length]
  (map #(to-closed-byte-range % total-body-length) ranges))

(defn validate-ranges
  "Checks that ranges are not overlapping relative to each other.
  NOTE: because the total body length may not be known, byte offsets cannot be absolutely checked.
  This means at most one of a suffix byte range or open-ended byte range can exist.

  If the body length is known, the ranges are checked against the body length."
  ([ranges]
   (let [sorted-ranges (sort-byte-ranges ranges)]
     (when (and (<= 1 (count ranges) max-num-ranges)
                (every? valid? sorted-ranges)
                (every? (fn [idx]
                          (let [prev-range (nth ranges idx)
                                next-range (nth ranges (inc idx))
                                prev-range-is-closed (instance? ClosedByteRange prev-range)
                                next-range-is-closed (instance? ClosedByteRange next-range)]
                            (if (and prev-range-is-closed next-range-is-closed)
                              (< (:last-byte-pos prev-range) (:first-byte-pos next-range))
                              (or prev-range-is-closed next-range-is-closed))))
                        (->> ranges (count) (dec) (range))))
       ranges)))
  ([ranges total-body-length]
   (let [ranges (convert-ranges-to-closed-byte ranges total-body-length)]
     (when (and (some? (validate-ranges ranges))
                (every? #(valid? % total-body-length) ranges))
       ranges))))

(defn- is-bytes?
  "This function is called `bytes?` and was added in Clojure 1.9; adding it here for backwards compatibility"
  [x]
  (if-let [clojure-bytes-fn (resolve 'clojure.core/bytes?)]
    (clojure-bytes-fn x)
    (if (nil? x)
      false
      (-> x class .getComponentType (= Byte/TYPE)))))

(defprotocol IContentLengthPrecalculator
  (content-length [this] "Precalculates the content length before the body is written"))

(defprotocol IBufferableBody
  (buffer [this response] "Start buffering until the body is read. Returns the number of bytes read."))

(defn limited-size-byte-array-output-stream
  [suggested-max-size]
  (let [max-size (min suggested-max-size max-buffer-size-per-range-bytes)
        circular-buffer (atom (vector))
        circular-buffer-pointer (atom 0)
        circular-buffer-size (fn [] (count @circular-buffer))
        handle-write (fn [byte-arr offset length]
                       (let [bytes-to-take (min max-size length)
                             bytes-end (+ offset length)
                             bytes-start (- bytes-end bytes-to-take)]
                         (loop [buffer @circular-buffer
                                byte-offset bytes-start
                                buffer-pointer @circular-buffer-pointer]
                           (if (>= byte-offset bytes-end)
                             (do (reset! circular-buffer buffer)
                                 (reset! circular-buffer-pointer buffer-pointer))
                             (let [next-item (nth byte-arr byte-offset)
                                   updated-buffer (if (< (count buffer) max-size)
                                                    (conj buffer next-item)
                                                    (assoc buffer buffer-pointer next-item))]
                               (recur updated-buffer
                                      (inc byte-offset)
                                      (mod (inc buffer-pointer) max-size)))))))
        limited-byte-array-output-stream (proxy [ByteArrayOutputStream] []
                                           (size []
                                             (circular-buffer-size))
                                           (write
                                             ([byte-arr]
                                              (handle-write byte-arr 0 (alength byte-arr)))
                                             ([byte-arr offset length]
                                              (handle-write byte-arr offset length)))
                                           (writeTo [^OutputStream output-stream]
                                             (.write output-stream (byte-array (subvec @circular-buffer @circular-buffer-pointer)))
                                             (.write output-stream (byte-array (subvec @circular-buffer 0 @circular-buffer-pointer)))))]
    limited-byte-array-output-stream))

(defn str-num-bytes
  [string]
  (-> string (.getBytes) (alength)))

(defn- content-length-calculator
  [ranges {:keys [total-body-length content-type boundary-str]}]
  (if (= 1 (count ranges))
    (length (first ranges) total-body-length)
    (->> (map (fn [range]
                (let [num-range-bytes (length range total-body-length)
                      content-type-length (str-num-bytes (format "Content-Type: %s" content-type))
                      content-range-length (str-num-bytes (format "Content-Range: %s" (content-range range total-body-length)))
                      ;; newline for after Content-Type, Content-Range, blank line, and bytes
                      newlines-length (* 4 (str-num-bytes multipart-newline))]
                  (+ num-range-bytes content-type-length content-range-length newlines-length)))
              ranges)
         (reduce + 0)
         (+ (* (str-num-bytes (format "--%s%s" boundary-str multipart-newline))
               (count ranges))
            (str-num-bytes (format "--%s--" boundary-str))))))

(defn- remove-idx-from-vec
  [vector idx]
  (vec (concat (into [] (subvec vector 0 idx))
               (into [] (subvec vector (inc idx))))))

(defrecord BufferingRangeBody [original-body ranges content-type boundary-str state]
  IBufferableBody
  (buffer [this response]
    (reset! state {:ranges (vec ranges)
                   :byte-buffers (->> ranges
                                      (map (fn [range]
                                             (if (instance? SuffixByteRange range)
                                               (limited-size-byte-array-output-stream (:suffix-length range))
                                               (ByteArrayOutputStream.))))
                                      (vec))})
    (let [bytes-read (atom 0)
          handle-write (fn [bytes bytes-offset bytes-length]
                         (doall
                           (map (fn [idx range buffer]
                                  (let [bytes-to-write (anticipated-number-of-bytes-to-write-to-stream range bytes bytes-offset bytes-length @bytes-read)]
                                    (if (> (+ (.size buffer) bytes-to-write)
                                           max-buffer-size-per-range-bytes)
                                      (do (swap! state update :ranges remove-idx-from-vec idx)
                                          (swap! state update :byte-buffers remove-idx-from-vec idx))
                                      (maybe-write-bytes-to-stream range bytes bytes-offset bytes-length @bytes-read buffer))))
                                (range (count (:ranges @state)))
                                (:ranges @state)
                                (:byte-buffers @state)))
                         (swap! bytes-read + bytes-length))
          proxied-stream (proxy [OutputStream] []
                           (write
                             ([byte-arr]
                              (handle-write byte-arr 0 (alength byte-arr)))
                             ([byte-arr offset length]
                              (handle-write byte-arr offset length))))]
      (write-body-to-stream original-body response proxied-stream)
      (when (empty? (validate-ranges (:ranges @state) @bytes-read))
        (reset! state {}))
      (swap! state assoc :total-body-length @bytes-read)
      {:total-body-length @bytes-read
       :num-valid-ranges (-> state (deref) :ranges (count))}))
  IContentLengthPrecalculator
  (content-length [this]
    (let [{:keys [total-body-length ranges]} (deref state)]
      (content-length-calculator ranges
                                 {:total-body-length total-body-length
                                  :content-type content-type
                                  :boundary-str boundary-str})))
  StreamableResponseBody
  (write-body-to-stream [this response output-stream]
    (let [{:keys [ranges byte-buffers total-body-length]} @state]
      (cond
        (= 1 (count ranges))
        (-> byte-buffers (first) (.writeTo output-stream))

        (< 1 (count ranges))
        (do
          (doall
            (map (fn [range buffer]
                   (.write output-stream (.getBytes (str/join multipart-newline
                                                              [(str "--" boundary-str)
                                                               (format "Content-Type: %s" content-type)
                                                               (format "Content-Range: %s" (content-range range total-body-length))
                                                               ""
                                                               ""])))
                   (.writeTo buffer output-stream)
                   (.write output-stream (.getBytes multipart-newline)))
                 ranges
                 byte-buffers))
          (.write output-stream (.getBytes (str "--" boundary-str "--"))))))))

(defrecord StreamingRangeBody [original-body ranges total-body-length content-type boundary-str]
  IContentLengthPrecalculator
  (content-length [this]
    (content-length-calculator ranges
                               {:total-body-length total-body-length
                                :content-type content-type
                                :boundary-str boundary-str}))
  StreamableResponseBody
  (write-body-to-stream [this response output-stream]
    (let [ranges (atom (-> ranges
                           (convert-ranges-to-closed-byte total-body-length)
                           (sort-byte-ranges)))
          has-more-than-one-range? (> (count @ranges) 1)
          bytes-read (atom 0)
          handle-write (fn [^bytes bytes offset length]
                         (let [new-bytes-read (+ length @bytes-read)
                               bytes-absolute-start-offset @bytes-read
                               bytes-absolute-end-offset (dec new-bytes-read)
                               bytes-absolute->relative-offset (- offset bytes-absolute-start-offset)]
                           (loop []
                             (when-let [{:keys [first-byte-pos last-byte-pos] :as first-range} (first @ranges)]
                               (let [range-start (max first-byte-pos bytes-absolute-start-offset)
                                     range-end (min last-byte-pos bytes-absolute-end-offset)
                                     should-write? (<= range-start range-end)]
                                 (when (and (<= bytes-absolute-start-offset first-byte-pos bytes-absolute-end-offset)
                                            has-more-than-one-range?)
                                   (.write output-stream (.getBytes (str/join multipart-newline
                                                                              [(format "--%s" boundary-str)
                                                                               (format "Content-Type: %s" content-type)
                                                                               (format "Content-Range: %s" (content-range first-range total-body-length)) ""
                                                                               ""]))))
                                 (when should-write?
                                   (.write output-stream
                                           bytes
                                           (+ range-start bytes-absolute->relative-offset)
                                           (inc (- range-end range-start))))
                                 (when (and (<= bytes-absolute-start-offset last-byte-pos bytes-absolute-end-offset)
                                            has-more-than-one-range?)
                                   (.write output-stream (.getBytes multipart-newline))
                                   (swap! ranges rest)
                                   (if (empty? @ranges)
                                     (.write output-stream (.getBytes (format "--%s--" boundary-str)))
                                     (recur))))))
                           (reset! bytes-read new-bytes-read)))
          proxied-stream (proxy [OutputStream] []
                           (write
                             ([byte-arr]
                              (handle-write byte-arr 0 (alength byte-arr)))
                             ([byte-arr offset length]
                              (handle-write byte-arr offset length))))]
      (write-body-to-stream original-body response proxied-stream))))

(defn add-accept-ranges-header-to-response
  [response]
  (header response "Accept-Ranges" bytes-unit))

(defn- add-headers-to-response
  ([response opts] (add-headers-to-response response nil opts))
  ([response ranges {:keys [total-body-length boundary-str] :as opts}]
   (let [{:keys [body] :as response} (add-accept-ranges-header-to-response response)]
     (case (:status response)
       200
       (cond
         (= 1 (count ranges))
         (-> response
             (status 206)
             (header "Content-Length" (content-length body))
             (header "Content-Range" (content-range (first ranges) total-body-length)))

         (< 1 (count ranges))
         (-> response
             (status 206)
             (header "Content-Length" (content-length body))
             (header "Content-Type" (str "multipart/byteranges; boundary=" boundary-str)))

         :else
         (add-headers-to-response (status 416) opts))

       416
       (header response "Content-Range" (format "%s */%d"
                                                bytes-unit
                                                total-body-length))

       response))))

(defn- unsatisfiable-response
  [total-body-length]
  (add-headers-to-response (status 416) {:total-body-length total-body-length}))

(defn- response+ranges->complete-response
  [{:keys [body] :as response} ranges total-body-length boundary-generator-fn]
  (let [boundary-str (when (> (count ranges) 1)
                       (boundary-generator-fn))
        content-type (get-header response "Content-Type")]
    (if (some? total-body-length)
      (-> response
          (assoc :body (map->StreamingRangeBody {:original-body body
                                                 :ranges ranges
                                                 :total-body-length total-body-length
                                                 :content-type content-type
                                                 :boundary-str boundary-str}))
          (add-headers-to-response ranges {:total-body-length total-body-length
                                           :boundary-str boundary-str}))
      (let [buffering-body (map->BufferingRangeBody {:original-body body
                                                     :ranges ranges
                                                     :content-type content-type
                                                     :boundary-str boundary-str
                                                     :state (atom {})})
            {:keys [total-body-length num-valid-ranges]} (buffer buffering-body response)]
        (if (= 0 num-valid-ranges)
          (unsatisfiable-response total-body-length)
          (-> response
              (assoc :body buffering-body)
              (add-headers-to-response ranges {:total-body-length total-body-length
                                               :boundary-str boundary-str})))))))

(defn find-body-content-length
  [{:keys [body] :as response}]
  (cond
    (string? body) (-> body (.getBytes) (alength))
    (is-bytes? body) (alength body)
    (instance? File body) (.length body)
    :else (maybe-parse-long (get-header response "Content-Length"))))

(defn- ensure-response-has-content-type-if-multirange
  [response request ranges]
  (if (> (count ranges) 1)
    (content-type-response response request)
    response))

(defn range-header-response
  "Fulfills the Range header request. See: wrap-range-header."
  [response request {:keys [boundary-generator-fn]}]
  (let [range-header (get-header request "Range")
        total-body-length (find-body-content-length response)]
    (if (and (= 200 (:status response))
             (= :get (:request-method request))
             (some? range-header))
      (let [parsed-ranges (header-value->byte-ranges range-header)
            validated-ranges (if (some? total-body-length)
                               (validate-ranges parsed-ranges total-body-length)
                               (validate-ranges parsed-ranges))]
        (if (empty? validated-ranges)
          (unsatisfiable-response total-body-length)
          (-> response
              (ensure-response-has-content-type-if-multirange request validated-ranges)
              (response+ranges->complete-response validated-ranges total-body-length boundary-generator-fn))))
      (add-accept-ranges-header-to-response response))))

(defn- char-range
  [start end]
  (map char (range (int start) (inc (int end)))))

(def boundary-chars (vec (concat (char-range \A \Z) (char-range \a \z) (char-range \0 \9))))

(defn boundary-generator
  []
  (->> (repeatedly #(rand-int (count boundary-chars)))
       (take 30)
       (map #(nth boundary-chars %))
       (apply str)))

(defn wrap-range-header
  "Middleware that attempts to fulfill the Range header in the request, if any.

  If the requested Range has valid byte offsets and no overlapping ranges, returns 206 with the requested bytes.
  Otherwise, returns the original response.

  A single range is returned directly as bytes in the body. Multiple ranges are returned as a multipart/byteranges
  response as per RFC7233.

  Accepts the following options:

  :boundary-generator-fn - a function that returns a boundary string for multipart responses.
                           Defaults to a randomly generated alphanumeric string"
  ([handler] (wrap-range-header handler {}))
  ([handler opts]
   (let [filtered-opts (merge {:boundary-generator-fn boundary-generator}
                              (select-keys opts [:boundary-generator-fn]))]
     (fn
       ([request]
        (range-header-response (handler request) request filtered-opts))
       ([request respond raise]
        (handler request
                 (fn [response] (respond (range-header-response response request filtered-opts)))
                 raise))))))

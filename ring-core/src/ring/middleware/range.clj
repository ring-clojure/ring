(ns ring.middleware.range
  "Middleware that handles HTTP range requests"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ring.core.protocols :refer :all]
            [ring.middleware.content-type :refer [content-type-response]]
            [ring.util.response :refer [status get-header header]])
  (:import [java.io ByteArrayOutputStream File IOException InputStream OutputStream RandomAccessFile]))

(defn- char-range
  [start end]
  (map char (range (int start) (inc (int end)))))

(def boundary-chars (vec (concat (char-range \A \Z) (char-range \a \z) (char-range \0 \9))))

(def ^:dynamic *boundary-generator* (fn []
                                      (->> (repeatedly #(rand-int (count boundary-chars)))
                                           (take 30)
                                           (map #(nth boundary-chars %))
                                           (apply str))))

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

(defn- maybe-parse-long
  [x]
  (some-> x (Long/parseLong)))

(defn header-value->ranges
  [^String header-value]
  (when (and (some? header-value)
             (some? (re-matches byte-ranges-specifier-regex header-value)))
    (->> (re-seq byte-or-suffix-byte-range-spec-regex header-value)
         (map (fn [[_ first-byte-pos last-byte-pos suffix-byte-length]]
                {:first-byte-pos (maybe-parse-long first-byte-pos)
                 :last-byte-pos (maybe-parse-long last-byte-pos)
                 :suffix-byte-length (maybe-parse-long suffix-byte-length)})))))

(defn sort-byte-ranges
  [ranges]
  (sort-by (juxt :suffix-byte-length :first-byte-pos :last-byte-pos) ranges))

(defn suffix-byte-range?
  [{:keys [suffix-byte-length] :as range}]
  (int? suffix-byte-length))

(defn open-ended-byte-range?
  [{:keys [first-byte-pos last-byte-pos] :as range}]
  (and (int? first-byte-pos)
       (nil? last-byte-pos)))

(defn range->start-byte
  [{:keys [first-byte-pos last-byte-pos suffix-byte-length] :as range} total-body-length]
  (if (suffix-byte-range? range)
    (- total-body-length suffix-byte-length)
    first-byte-pos))

(defn range->end-byte
  [{:keys [first-byte-pos last-byte-pos suffix-byte-length]} total-body-length]
  (if (int? last-byte-pos)
    last-byte-pos
    (dec total-body-length)))

(defn has-nonoverlapping-ranges-except-for-last-suffix-range?
  "Checks that ranges are not overlapping, EXCEPT for the case where a suffix range is given.
  Because the body is parsed a stream, the total length is unknown and so it is known if the suffix bytes
  will overlap with the other given ranges.

  Checks that the other ranges are not overlapping. That also means at most one of a suffix byte range or
  open-ended byte range can exist."
  [ranges]
  (let [sorted-ranges (sort-byte-ranges ranges)]
    (and (not (empty? ranges))
         (every? (fn [{:keys [first-byte-pos last-byte-pos suffix-byte-length]}]
                   (or (and (int? first-byte-pos)
                            (int? last-byte-pos)
                            (<= first-byte-pos last-byte-pos))
                       (int? first-byte-pos)
                       (int? suffix-byte-length)))
                 sorted-ranges)
         (every? (fn [idx]
                   (let [prev-range (nth ranges idx)
                         next-range (nth ranges (inc idx))]
                     (not (and (or (open-ended-byte-range? prev-range)
                                   (suffix-byte-range? prev-range))
                               (or (open-ended-byte-range? next-range)
                                   (suffix-byte-range? next-range))))))
                 (->> ranges (count) (dec) (range))))))

(defn parse-ranges
  [header-value]
  (when-let [ranges (header-value->ranges header-value)]
    (when (has-nonoverlapping-ranges-except-for-last-suffix-range? ranges)
      ranges)))

(defn response+ranges->bytes
  [{:keys [body] :as response} ranges]
  (let [byte-position-read (atom -1)
        min-open-ended-range-start (->> ranges
                                        (filter open-ended-byte-range?)
                                        (map :first-byte-pos)
                                        (sort)
                                        (first))
        open-ended-byte-range-stream (ByteArrayOutputStream.)

        max-suffix-byte-range-length (->> ranges
                                          (filter suffix-byte-range?)
                                          (map :suffix-byte-length)
                                          (sort)
                                          (last))
        suffix-byte-range-buffer (atom (vector))

        other-ranges->bytes (->> ranges
                                 (filter #(not (or (open-ended-byte-range? %)
                                                   (suffix-byte-range? %))))
                                 (map (fn [range]
                                        [range (atom (vector))]))
                                 (into {}))
        handle-write (fn [^bytes byte-arr]
                       (let [length (alength byte-arr)
                             byte-arr-start-position (inc @byte-position-read)
                             byte-arr-end-position (+ @byte-position-read length)]
                         (when min-open-ended-range-start
                           (cond
                             ; bytes:    |-----
                             ; range: |-------
                             (>= byte-arr-start-position min-open-ended-range-start)
                             (.write open-ended-byte-range-stream byte-arr)

                             ; bytes: |---|
                             ; range:   |-------
                             (>= byte-arr-end-position min-open-ended-range-start byte-arr-start-position)
                             (.write open-ended-byte-range-stream ^bytes (-> byte-arr
                                                                             (vec)
                                                                             (subvec (+ (dec length)
                                                                                        min-open-ended-range-start
                                                                                        (- byte-arr-end-position)))
                                                                             (byte-array)))))
                         (when max-suffix-byte-range-length
                           (if (>= length max-suffix-byte-range-length)
                             (reset! suffix-byte-range-buffer (-> byte-arr
                                                                  (vec)
                                                                  (subvec (- length max-suffix-byte-range-length))))
                             (let [combined-length (+ (count @suffix-byte-range-buffer) length)]
                               (if (> combined-length max-suffix-byte-range-length)
                                 (do
                                   (swap! suffix-byte-range-buffer subvec (- combined-length max-suffix-byte-range-length))
                                   (swap! suffix-byte-range-buffer concat byte-arr))
                                 (swap! suffix-byte-range-buffer concat byte-arr)))))
                         (doseq [[{:keys [first-byte-pos last-byte-pos]} range-bytes-atom] other-ranges->bytes]
                           (cond
                             ; bytes: |---------|
                             ; range:   |-----|
                             (and (<= byte-arr-start-position first-byte-pos)
                                  (<= last-byte-pos byte-arr-end-position))
                             (reset! range-bytes-atom (-> byte-arr
                                                          (vec)
                                                          (subvec (- first-byte-pos byte-arr-start-position)
                                                                  (inc (- last-byte-pos byte-arr-start-position)))))

                             ; bytes: |-----|
                             ; range:   |-----|
                             (and (<= byte-arr-start-position first-byte-pos byte-arr-end-position)
                                  (<= first-byte-pos byte-arr-end-position last-byte-pos))
                             (swap! range-bytes-atom concat (-> byte-arr
                                                                (vec)
                                                                (subvec (- first-byte-pos byte-arr-start-position))))
                             ; bytes:    |-----|
                             ; range:  |-----|
                             ; OR
                             ; bytes:    |--|
                             ; range:  |------|
                             (and (<= first-byte-pos byte-arr-start-position last-byte-pos)
                                  (<= byte-arr-start-position last-byte-pos byte-arr-end-position))
                             (swap! range-bytes-atom concat (-> byte-arr
                                                                (vec)
                                                                (subvec 0
                                                                        (min length
                                                                             (inc (- last-byte-pos @byte-position-read))))))))
                         (reset! byte-position-read byte-arr-end-position)))
        output-stream (proxy [OutputStream] []
                        (write
                          ([byte-arr]
                           (handle-write byte-arr))
                          ([byte-arr offset length]
                           (handle-write (-> byte-arr
                                             (vec)
                                             (subvec offset (+ offset length))
                                             (byte-array))))))]
    (write-body-to-stream body response output-stream)
    (when (every? (fn [{:keys [first-byte-pos last-byte-pos suffix-byte-length] :as range}]
                    (cond
                      (suffix-byte-range? range) (<= suffix-byte-length (inc @byte-position-read))
                      (open-ended-byte-range? range) (<= first-byte-pos @byte-position-read)
                      :else (<= last-byte-pos @byte-position-read)))
                  ranges)
      (let [open-ended-bytes (vec (.toByteArray open-ended-byte-range-stream))]
        {:total-bytes-read (inc @byte-position-read)
         :range-bytes (->> ranges
                           (map (fn [{:keys [first-byte-pos last-byte-pos suffix-byte-length] :as range}]
                                  [range (byte-array (cond
                                                       (open-ended-byte-range? range)
                                                       (subvec open-ended-bytes (- first-byte-pos min-open-ended-range-start))

                                                       (suffix-byte-range? range)
                                                       (subvec @suffix-byte-range-buffer (- max-suffix-byte-range-length suffix-byte-length))

                                                       :else
                                                       (byte-array @(get other-ranges->bytes range))))]))
                           (into {}))}))))

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
      (let [ranges (header-value->ranges range-header)]
        (if (has-nonoverlapping-ranges-except-for-last-suffix-range? ranges)
          (if-let [{:keys [total-bytes-read range-bytes]} (response+ranges->bytes response ranges)]
            (cond
              (= 1 (count range-bytes))
              (let [[range ^bytes bytes] (first range-bytes)]
                (-> response
                    (status 206)
                    (header "Content-Length" (alength bytes))
                    (header "Content-Range" (format "bytes %d-%d/%d"
                                                    (range->start-byte range total-bytes-read)
                                                    (range->end-byte range total-bytes-read)
                                                    total-bytes-read))
                    (assoc :body (String. bytes))))

              (< 1 (count range-bytes))
              (let [response-with-content-type (content-type-response response request)
                    content-type (get-header response-with-content-type "Content-Type")
                    multipart-boundary-str (*boundary-generator*)
                    body (->> range-bytes
                              (map (fn [[range ^bytes bytes]]
                                     (str/join "\r\n" [(format "Content-Type: %s" content-type)
                                                       (format "Content-Range: %d-%d/%d"
                                                               (range->start-byte range total-bytes-read)
                                                               (range->end-byte range total-bytes-read)
                                                               total-bytes-read)
                                                       ""
                                                       (String. bytes)])))
                              (str/join (format "\r\n--%s\r\n" multipart-boundary-str)))
                    body (str/join "\r\n" [(str "--" multipart-boundary-str)
                                           body
                                           (str "--" multipart-boundary-str "--")])]
                (-> response-with-content-type
                    (status 206)
                    (header "Content-Length" (-> body (.getBytes) (alength)))
                    (header "Content-Type" (format "multipart/byteranges; boundary=%s" multipart-boundary-str))
                    (assoc :body body))))
            response)
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
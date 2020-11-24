(ns ring.middleware.range
  "Middleware that handles HTTP range requests"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ring.core.protocols :refer :all]
            [ring.middleware.content-type :refer [content-type-response]]
            [ring.util.response :refer [status get-header header]])
  (:import [java.io ByteArrayOutputStream OutputStream]))

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

(defn header-value->byte-ranges
  "If the header is valid, turns the header into a seq of ranges."
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

(defn closed-byte-range?
  [{:keys [first-byte-pos last-byte-pos] :as range}]
  (and (int? first-byte-pos)
       (int? last-byte-pos)) )

(defn suffix-byte-range?
  [{:keys [suffix-byte-length] :as range}]
  (int? suffix-byte-length))

(defn open-ended-byte-range?
  [{:keys [first-byte-pos last-byte-pos] :as range}]
  (and (int? first-byte-pos)
       (nil? last-byte-pos)))

(defn range-valid?
  [{:keys [first-byte-pos last-byte-pos suffix-byte-length] :as range}]
  (if (and (int? first-byte-pos) (int? last-byte-pos))
    (<= first-byte-pos last-byte-pos)
    true))

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

(defn range->content-range-header
  [range total-byte-length]
  (format "bytes %d-%d/%d"
          (range->start-byte range total-byte-length)
          (range->end-byte range total-byte-length)
          total-byte-length))

(defn suffix-byte-range->closed-byte-range
  [{:keys [suffix-byte-length] :as range} total-length]
  {:pre [(suffix-byte-range? range)]}
  {:first-byte-pos (- total-length suffix-byte-length)
   :last-byte-pos (dec total-length)})

(defn open-ended-byte-range->closed-byte-range
  [{:keys [first-byte-pos] :as range} total-length]
  {:pre [(open-ended-byte-range? range)]}
  {:first-byte-pos first-byte-pos
   :last-byte-pos (dec total-length)})

(defn convert-ranges-to-closed-byte
  [ranges total-length]
  (->> ranges
       (map #(cond
               (open-ended-byte-range? %)
               (open-ended-byte-range->closed-byte-range % total-length)

               (suffix-byte-range? %)
               (suffix-byte-range->closed-byte-range % total-length)

               :else
               %))))

(defn validate-has-nonoverlapping-ranges
  "Checks that ranges are not overlapping, EXCEPT for the case where a suffix range is given.
  Because the body is parsed a stream, the total length is unknown and so it is known if the suffix bytes
  will overlap with the other given ranges.

  Checks that the other ranges are not overlapping. That also means at most one of a suffix byte range or
  open-ended byte range can exist."
  [ranges]
  (let [sorted-ranges (sort-byte-ranges ranges)]
    (when (and (not (empty? ranges))
               (every? range-valid? sorted-ranges)
               (every? (fn [idx]
                         (let [prev-range (nth ranges idx)
                               next-range (nth ranges (inc idx))
                               prev-range-is-closed (closed-byte-range? prev-range)
                               next-range-is-closed (closed-byte-range? next-range)]
                           (if (and prev-range-is-closed next-range-is-closed)
                             (< (:last-byte-pos prev-range) (:first-byte-pos next-range))
                             (or prev-range-is-closed next-range-is-closed))))
                       (->> ranges (count) (dec) (range))))
      ranges)))

(defn ensure-response-has-content-type-if-multirange
  [response request ranges]
  [(if (> (count ranges) 1)
     (content-type-response response request)
     response)
   ranges])

(defprotocol IRangeByteContainer
  "A container that holds a Range and attempts to fetch its demarked bytes from a stream."
  (satisfied? [_])
  (get-bytes [_])
  (write [_ bytes byte-start-offset] "Write bytes to the container."))

(defrecord SuffixRangeByteContainer [suffix-byte-length byte-store]
  IRangeByteContainer
  (satisfied? [_]
    (>= (count @byte-store) suffix-byte-length))
  (get-bytes [_]
    @byte-store)
  (write [_ bytes _]
    (let [length (alength bytes)
          existing-length (count @byte-store)
          total-length (+ length existing-length)]
      (reset! byte-store (-> (into @byte-store bytes)
                             (vec)
                             (subvec (max 0 (- total-length suffix-byte-length))))))))

(defrecord OpenEndedRangeByteContainer [first-byte-pos byte-store]
  IRangeByteContainer
  (satisfied? [_]
    (>= (count @byte-store) 0))
  (get-bytes [_]
    @byte-store)
  (write [_ bytes byte-start-offset]
    (if (>= byte-start-offset first-byte-pos)
      (reset! byte-store (into @byte-store bytes))
      (let [length (alength bytes)
            bytes-to-take (max 0
                               (- (+ byte-start-offset length)
                                  first-byte-pos))]
        (when (> bytes-to-take 0)
          (->> (subvec (vec bytes) (+ byte-start-offset
                                      length
                                      (- bytes-to-take)))
               (into @byte-store)
               (reset! byte-store)))))))

(defrecord ClosedRangeByteContainer [first-byte-pos last-byte-pos byte-store]
  IRangeByteContainer
  (satisfied? [_]
    (= (count @byte-store) (inc (- last-byte-pos first-byte-pos))))
  (get-bytes [_]
    @byte-store)
  (write [_ bytes byte-start-offset]
    (let [length (alength bytes)
          byte-end-offset (dec (+ length byte-start-offset))
          start-offset-to-take (max first-byte-pos byte-start-offset)
          end-offset-to-take (min last-byte-pos byte-end-offset)]
      (when (<= start-offset-to-take end-offset-to-take)
        (->> (subvec (vec bytes)
                     start-offset-to-take
                     (inc end-offset-to-take))
             (into @byte-store)
             (reset! byte-store))))))

(defn response+ranges->response+range-bytes
  [{:keys [body] :as response} ranges]
  (let [byte-position-read (atom -1)
        open-ended-byte-container (when-let [min-first-byte-pos (->> ranges
                                                                     (filter open-ended-byte-range?)
                                                                     (map :first-byte-pos)
                                                                     (sort)
                                                                     (first))]
                                    (OpenEndedRangeByteContainer. min-first-byte-pos (atom [])))
        suffix-byte-container (when-let [max-suffix-length (->> ranges
                                                                (filter suffix-byte-range?)
                                                                (map :suffix-byte-length)
                                                                (sort)
                                                                (last))]
                                (SuffixRangeByteContainer. max-suffix-length (atom [])))
        other-ranges->byte-containers (->> ranges
                                           (filter #(not (or (open-ended-byte-range? %)
                                                             (suffix-byte-range? %))))
                                           (map (fn [{:keys [first-byte-pos last-byte-pos] :as range}]
                                                  [range (ClosedRangeByteContainer. first-byte-pos last-byte-pos (atom []))]))
                                           (into {}))
        all-byte-containers (filter some? (conj (vals other-ranges->byte-containers)
                                                open-ended-byte-container
                                                suffix-byte-container))
        handle-write (fn [^bytes bytes]
                       (let [bytes-start-offset (inc @byte-position-read)
                             bytes-end-offset (+ @byte-position-read (alength bytes))]
                         (doseq [byte-container all-byte-containers]
                           (write byte-container bytes bytes-start-offset))
                         (reset! byte-position-read bytes-end-offset)))
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
    (when (and (every? satisfied? all-byte-containers)
               (validate-has-nonoverlapping-ranges (convert-ranges-to-closed-byte ranges (inc @byte-position-read))))
      (let [open-ended-bytes (some-> open-ended-byte-container (get-bytes))
            suffix-bytes (some-> suffix-byte-container (get-bytes))]
        [response
         {:total-bytes-read (inc @byte-position-read)
          :range-to-bytes-map (->> ranges
                                   (map (fn [{:keys [first-byte-pos last-byte-pos suffix-byte-length] :as range}]
                                          [range (byte-array (cond
                                                               (open-ended-byte-range? range)
                                                               (subvec open-ended-bytes (- first-byte-pos (:first-byte-pos open-ended-byte-container)))

                                                               (suffix-byte-range? range)
                                                               (subvec suffix-bytes (- (:suffix-byte-length suffix-byte-container) suffix-byte-length))

                                                               :else
                                                               (->> range
                                                                    (get other-ranges->byte-containers)
                                                                    (get-bytes))))]))
                                   (into {}))}]))))

(defn response+range-bytes->complete-response
  [boundary-generator-fn response {:keys [total-bytes-read range-to-bytes-map]}]
  (cond
    (= 1 (count range-to-bytes-map))
    (let [[range ^bytes bytes] (first range-to-bytes-map)]
      (-> response
          (status 206)
          (header "Content-Length" (alength bytes))
          (header "Content-Range" (range->content-range-header range total-bytes-read))
          (assoc :body (String. bytes))))

    (< 1 (count range-to-bytes-map))
    (let [multipart-boundary-str (boundary-generator-fn)
          body (->> range-to-bytes-map
                    (map (fn [[range ^bytes bytes]]
                           (str/join "\r\n" [(format "Content-Type: %s" (get-header response "Content-Type"))
                                             (format "Content-Range: %s" (range->content-range-header range total-bytes-read))
                                             ""
                                             (String. bytes)])))
                    (str/join (format "\r\n--%s\r\n" multipart-boundary-str)))
          body (str/join "\r\n" [(str "--" multipart-boundary-str)
                                 body
                                 (str "--" multipart-boundary-str "--")])]
      (-> response
          (status 206)
          (header "Content-Length" (-> body (.getBytes) (alength)))
          (header "Content-Type" (format "multipart/byteranges; boundary=%s" multipart-boundary-str))
          (assoc :body body)))

    :else
    response))

(defn range-header-response
  "Returns the original response if no Range header present or if the header is invalid.
  If one range is given, returns 206 with requested bytes in the body.
  If multiple ranges are requested, returns a 206 with a multipart/byteranges body."
  [response request {:keys [boundary-generator-fn]}]
  (let [response (header response "Accept-Ranges" "bytes")
        range-header (get-header request "Range")]
    (or (when (and (= 200 (:status response))
                   (= :get (:request-method request))
                   (some? range-header))
          (some->> (header-value->byte-ranges range-header)
                   (validate-has-nonoverlapping-ranges)
                   (ensure-response-has-content-type-if-multirange response request)
                   (apply response+ranges->response+range-bytes)
                   (apply response+range-bytes->complete-response boundary-generator-fn)))
        response)))

(defn- char-range
  [start end]
  (map char (range (int start) (inc (int end)))))

(def boundary-chars (vec (concat (char-range \A \Z) (char-range \a \z) (char-range \0 \9))))

(def boundary-generator (fn []
                          (->> (repeatedly #(rand-int (count boundary-chars)))
                               (take 30)
                               (map #(nth boundary-chars %))
                               (apply str))))

(defn wrap-range-header
  "Middleware that attempts to fulfill the Range header in the request, if any.

  If the requested Range has valid byte offsets and no overlapping ranges, returns 206 with the requested bytes.
  Otherwise, returns the original response.

  A single range is returned directly as bytes in the body. Multiple ranges are returned as a multipart/byteranges
  response as per RFC7233.

  Accepts the following options:

  :boundary-generator-fn - a function that returns a boundary string for multipart responses.
                           Defaults to a randomly generated alphanumeric string"
  ([handler] (wrap-range-header handler {:boundary-generator-fn boundary-generator}))
  ([handler opts]
   (let [filtered-opts (select-keys opts [:boundary-generator-fn])]
     (fn
       ([request]
        (range-header-response (handler request) request filtered-opts))
       ([request respond raise]
        (handler request
                 (fn [response] (respond (range-header-response response request filtered-opts)))
                 raise))))))
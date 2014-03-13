(ns ring.middleware.multipart-params
  "Parse multipart upload into params."
  (:import [java.io InputStream BufferedInputStream])
  (:use [ring.util.codec :only (assoc-conj)]
        [ring.util.parsing :onlt (re-value)])
  (:require [ring.util.request :as req]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [substream.core :as sub]))

(defn- multipart-form?
  "Does a request have a multipart form?"
  [request]
  (= (req/content-type request) "multipart/form-data"))

(defn- end-of-stream? [^BufferedInputStream stream]
  (.mark stream 1)
  (let [end? (= (.read stream) -1)]
    (.reset stream)
    end?))

(def re-boundary
  (re-pattern (str "boundary=(" re-value ")")))

(defn- multipart-boundary
  "Find the boundary of a multipart request."
  [request]
  (->> (get-in request [:headers "content-type"])
       (re-find re-boundary)
       (second)))

(defn- byte->int [b]
  (bit-and b 0xff))

(defn- stream-until-boundary [^BufferedInputStream stream boundary]
  (let [b-size (count boundary)]
    (.mark stream b-size)
    (sub/input-stream
     #(loop [i 0]
        (let [b1 (.read stream)
              b2 (byte->int (aget boundary i))]
          (if (= i b-size)
            (do (.read stream) (.read stream) -1)
            (if (= b1 b2)
              (recur (inc i))
              (do (doto stream .reset .read (.mark b-size)) b1))))))))

(def ^:private CR (byte 13))
(def ^:private LF (byte 10))

(defn- read-headers [stream]
  (let [boundary (byte-array [CR LF CR LF])
        stream   (stream-until-boundary stream boundary)]
    (->> (line-seq (io/reader stream))
         (map #(str/split % #":" 1))
         (reduce (fn [m [k v]] (assoc m (str/lower-case k) (str/trim v))) {}))))

(def re-disposition
  (re-pattern (str "(" re-token ")=(" re-value ")\\s*[;,]?")))

(defn- parse-disposition [disposition]
  
  (let [disposition (headers "conten")]))

(defn- parse-multipart-params [request encoding store]
  (let [boundary (-> (multipart-bounday request) (.getBytes encoding))]
    (with-open [stream (BufferedInputStream. (:body request))]
      (loop [params {}]
        (if (end-of-stream? stream)
          params
          (let [stream      (stream-until-boundary stream boundary)
                headers     (read-headers stream)
                disposition (parse-disposition (headers "content-disposition"))]
            (assoc-conj params
              (:name disposition)
              {:filename     (:filename disposition)
               :content-type (headers "content-type")
               :stream       stream})))))))

(defn- load-var
  "Returns the var named by the supplied symbol, or nil if not found. Attempts
  to load the var namespace on the fly if not already loaded."
  [sym]
  (require (symbol (namespace sym)))
  (find-var sym))

(def ^:private default-store
  (delay
   (let [store 'ring.middleware.multipart-params.temp-file/temp-file-store
         func  (load-var store)]
     (func))))

(defn multipart-params-request
  "Adds :multipart-params and :params keys to request."
  [request & [opts]]
  (let [store    (or (:store opts) @default-store)
        encoding (or (:encoding opts)
                     (req/character-encoding request)
                     "UTF-8")
        params   (if (multipart-form? request)
                   (parse-multipart-params request encoding store)
                   {})]
    (merge-with merge request
                {:multipart-params params}
                {:params params})))

(defn wrap-multipart-params
  "Middleware to parse multipart parameters from a request. Adds the
  following keys to the request map:
    :multipart-params - a map of multipart parameters
    :params           - a merged map of all types of parameter

  This middleware takes an optional configuration map. Recognized keys are:

    :encoding - character encoding to use for multipart parsing. If not
                specified, uses the request character encoding, or \"UTF-8\"
                if no request character encoding is set.

    :store    - a function that stores a file upload. The function should
                expect a map with :filename, content-type and :stream keys,
                and its return value will be used as the value for the
                parameter in the multipart parameter map. The default storage
                function is the temp-file-store."
  [handler & [opts]]
  (fn [request]
    (-> request
        (multipart-params-request opts)
        handler)))

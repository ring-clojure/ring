(ns ring.middleware.multipart-params
  "Parse multipart upload into params."
  (:use [ring.util.codec :only (assoc-conj)])
  (:require [ring.util.request :as req])
  (:import [org.apache.commons.fileupload.util Streams]
           [org.apache.commons.fileupload
             UploadContext
             FileItemIterator
             FileItemStream
             FileUpload]))

(defn- multipart-form?
  "Does a request have a multipart form?"
  [request]
  (= (req/content-type request) "multipart/form-data"))

(defn- request-context
  "Create an UploadContext object from a request map."
  {:tag UploadContext}
  [request encoding]
  (reify UploadContext
    (getContentType [this]       (get-in request [:headers "content-type"]))
    (getContentLength [this]     (or (:content-length request) -1))
    (contentLength [this]        (or (:content-length request) -1))
    (getCharacterEncoding [this] encoding)
    (getInputStream [this]       (:body request))))

(defn- file-item-iterator-seq
  "Create a lazy seq from a FileItemIterator instance."
  [^FileItemIterator it]
  (lazy-seq
    (if (.hasNext it)
      (cons (.next it) (file-item-iterator-seq it)))))

(defn- file-item-seq
  "Create a seq of FileItem instances from a request context."
  [context]
  (file-item-iterator-seq
    (.getItemIterator (FileUpload.) context)))

(defn- parse-file-item
  "Parse a FileItemStream into a key-value pair. If the request is a file the
  supplied store function is used to save it."
  [^FileItemStream item store]
  [(.getFieldName item)
   (if (.isFormField item)
     (Streams/asString (.openStream item))
     (store {:filename     (.getName item)
             :content-type (.getContentType item)
             :stream       (.openStream item)}))])

(defn- parse-multipart-params
  "Parse a map of multipart parameters from the request."
  [request encoding store]
  (->> (request-context request encoding)
       (file-item-seq)
       (map #(parse-file-item % store))
       (reduce (fn [m [k v]] (assoc-conj m k v)) {})))

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
                     (:character-encoding request)
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

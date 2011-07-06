(ns ring.middleware.multipart-params
  "Parse multipart upload into params."
  (:use [ring.middleware.params :only (assoc-param)])
  (:import [org.apache.commons.fileupload.util Streams]
           [org.apache.commons.fileupload
             RequestContext
             FileItemIterator
             FileItemStream
             FileUpload]))

(defn- multipart-form?
  "Does a request have a multipart form?"
  [request]
  (if-let [^String content-type (:content-type request)]
    (.startsWith content-type "multipart/form-data")))

(defn- request-context
  "Create a RequestContext object from a request map."
  {:tag RequestContext}
  [request encoding]
  (reify RequestContext
    (getContentType [this]       (:content-type request))
    (getContentLength [this]     (:content-length request))
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
  "Parse a FileItemStream into a parameter value. If the request is a file the
  supplied store function is used to save it."
  [^FileItemStream item store]
  (if (.isFormField item)
    (Streams/asString (.openStream item))
    (store {:filename     (.getName item)
            :content-type (.getContentType item)
            :stream       (.openStream item)})))

(defn- parse-multipart-params
  "Parse a map of multipart parameters from the request."
  [request encoding store]
  (into {}
    (for [item (file-item-seq (request-context request encoding))]
      [(.getFieldName item)
       (parse-file-item item store)])))

(defn- load-var
  "Returns the var named by the supplied symbol, or nil if not found. Attempts
  to load the var namespace on the fly if not already loaded."
  [sym]
  (require (symbol (namespace sym)))
  (find-var sym))

(defn default-store
  "Loads and returns a temporary file store."
  []
  (let [store 'ring.middleware.multipart-params.temp-file/temp-file-store
        func  (load-var store)]
    (func)))

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
    (let [encoding (or (:encoding opts)
                       (:character-encoding request)
                       "UTF-8")
          store    (or (:store opts)
                       (default-store))
          params   (if (multipart-form? request)
                     (parse-multipart-params request encoding store)
                     {})
          request  (merge-with merge request
                     {:multipart-params params}
                     {:params params})]
      (handler request))))

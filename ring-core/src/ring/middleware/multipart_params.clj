(ns ring.middleware.multipart-params
  "Middleware that parses multipart request bodies into parameters.

  This middleware is necessary to handle file uploads from web browsers.

  Ring comes with two different multipart storage engines included:

    ring.middleware.multipart-params.byte-array/byte-array-store
    ring.middleware.multipart-params.temp-file/temp-file-store"
  (:require [ring.util.codec :refer [assoc-conj]]
            [ring.util.request :as req])
  (:import [org.apache.commons.fileupload.util Streams]
           [org.apache.commons.fileupload UploadContext
                                          FileItemIterator
                                          FileItemStream
                                          FileUpload
                                          ProgressListener]))
(defn- progress-listener
  "Create a progress listener that calls the supplied function."
  [request progress-fn]
  (reify ProgressListener
    (update [this bytes-read content-length item-count]
      (progress-fn request bytes-read content-length item-count))))

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
    (getContentLength [this]     (or (req/content-length request) -1))
    (contentLength [this]        (or (req/content-length request) -1))
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
  [request ^ProgressListener progress-fn context]
  (let [upload (if progress-fn
                 (doto (FileUpload.)
                   (.setProgressListener (progress-listener request progress-fn)))
                 (FileUpload.))]
    (file-item-iterator-seq
      (.getItemIterator ^FileUpload upload context))))

(defn- parse-file-item
  "Parse a FileItemStream into a key-value pair. If the request is a file the
  supplied store function is used to save it."
  [^FileItemStream item store encoding]
  [(.getFieldName item)
   (if (.isFormField item)
     (Streams/asString (.openStream item) encoding)
     (store {:filename     (.getName item)
             :content-type (.getContentType item)
             :stream       (.openStream item)}))])

(defn- parse-multipart-params
  "Parse a map of multipart parameters from the request."
  [request encoding store progress-fn]
  (->> (request-context request encoding)
       (file-item-seq request progress-fn)
       (map #(parse-file-item % store encoding))
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
  "Adds :multipart-params and :params keys to request.
  See: wrap-multipart-params."
  {:arglists '([request] [request options])
   :added "1.2"}
  [request & [options]]
  (let [store    (or (:store options) @default-store)
        encoding (or (:encoding options)
                     (req/character-encoding request)
                     "UTF-8")
        progress (:progress-fn options)
        params   (if (multipart-form? request)
                   (parse-multipart-params request encoding store progress)
                   {})]
    (merge-with merge request
                {:multipart-params params}
                {:params params})))

(defn wrap-multipart-params
  "Middleware to parse multipart parameters from a request. Adds the
  following keys to the request map:

  :multipart-params - a map of multipart parameters
  :params           - a merged map of all types of parameter

  The following options are accepted

  :encoding    - character encoding to use for multipart parsing. If not
                 specified, uses the request character encoding, or \"UTF-8\"
                 if no request character encoding is set.

  :store       - a function that stores a file upload. The function should
                 expect a map with :filename, content-type and :stream keys,
                 and its return value will be used as the value for the
                 parameter in the multipart parameter map. The default storage
                 function is the temp-file-store.

  :progress-fn - a function that gets called during uploads. The function
                 should expect four parameters: request, bytes-read,
                 content-length, and item-count."
  {:arglists '([handler] [handler options])}
  [handler & [options]]
  (fn
    ([request]
     (handler (multipart-params-request request options)))
    ([request cont]
     (handler (multipart-params-request request options) cont))))

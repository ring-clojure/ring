(ns ring.middleware.multipart-params
  "Middleware that parses multipart request bodies into parameters.

  This middleware is necessary to handle file uploads from web browsers.

  Ring comes with two different multipart storage engines included:

    ring.middleware.multipart-params.byte-array/byte-array-store
    ring.middleware.multipart-params.temp-file/temp-file-store"
  (:require [ring.util.codec :refer [assoc-conj]]
            [ring.util.request :as req]
            [ring.util.parsing :refer [re-charset]])
  (:import [org.apache.commons.fileupload UploadContext
                                          FileItemIterator
                                          FileItemStream
                                          FileUpload
                                          ProgressListener]
           [org.apache.commons.io IOUtils]))
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
  [request progress-fn context]
  (let [upload (if progress-fn
                 (doto (FileUpload.)
                   (.setProgressListener (progress-listener request progress-fn)))
                 (FileUpload.))]
    (file-item-iterator-seq
      (.getItemIterator ^FileUpload upload context))))

(defn- parse-content-type-charset [^FileItemStream item]
  (some->> (.getContentType item)
           (re-find re-charset)
           second))

(defn- parse-html5-charset [params]
  (when-let [charset (->> params (filter #(= (first %) "_charset_")) first second :bytes)]
    (String. ^bytes charset "US-ASCII")))

(defn- decode-string-values [fallback-encoding forced-encoding params]
  (let [html5-encoding (parse-html5-charset params)]
    (for [[k v field?] params]
      [k (if field?
           (String. (:bytes v) (str (or forced-encoding
                                        html5-encoding
                                        (:encoding v)
                                        fallback-encoding)))
           v)])))

(defn- parse-file-item
  "Parse a FileItemStream into a key-value pair. If the request is a file the
  supplied store function is used to save it."
  [^FileItemStream item store]
  [(.getFieldName item)
   (if (.isFormField item)
     {:bytes    (IOUtils/toByteArray (.openStream item))
      :encoding (parse-content-type-charset item)}
     (store {:filename     (.getName item)
             :content-type (.getContentType item)
             :stream       (.openStream item)}))
   (.isFormField item)])

(defn- parse-multipart-params
  "Parse a map of multipart parameters from the request."
  [request fallback-encoding forced-encoding store progress-fn]
  (->> (request-context request fallback-encoding)
       (file-item-seq request progress-fn)
       (map #(parse-file-item % store))
       (decode-string-values fallback-encoding forced-encoding)
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
  {:added "1.2"}
  ([request]
   (multipart-params-request request {}))
  ([request options]
   (let [store           (or (:store options) @default-store)
         forced-encoding (:encoding options)
         req-encoding    (or forced-encoding
                             (:fallback-encoding options)
                             (req/character-encoding request)
                             "UTF-8")
         progress        (:progress-fn options)
         params          (if (multipart-form? request)
                           (parse-multipart-params request
                                                   req-encoding
                                                   forced-encoding
                                                   store
                                                   progress)
                           {})]
     (merge-with merge request
                 {:multipart-params params}
                 {:params params}))))

(defn wrap-multipart-params
  "Middleware to parse multipart parameters from a request. Adds the
  following keys to the request map:

  :multipart-params - a map of multipart parameters
  :params           - a merged map of all types of parameter

  The following options are accepted

  :encoding          - character encoding to use for multipart parsing.
                       Overrides the encoding specified in the request. If not
                       specified, uses the encoding specified in a part named
                       \"_charset_\", or the content type for each part, or
                       request character encoding if the part has no encoding,
                       or \"UTF-8\" if no request character encoding is set.

  :fallback-encoding - specifies the character encoding used in parsing if a
                       part of the request does not specify encoding in its
                       content type or no part named \"_charset_\" is present.
                       Has no effect if :encoding is also set.

  :store             - a function that stores a file upload. The function
                       should expect a map with :filename, content-type and
                       :stream keys, and its return value will be used as the
                       value for the parameter in the multipart parameter map.
                       The default storage function is the temp-file-store.

  :progress-fn       - a function that gets called during uploads. The
                       function should expect four parameters: request,
                       bytes-read, content-length, and item-count."
  ([handler]
   (wrap-multipart-params handler {}))
  ([handler options]
   (fn
     ([request]
      (handler (multipart-params-request request options)))
     ([request respond raise]
      (handler (multipart-params-request request options) respond raise)))))

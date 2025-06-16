(ns ring.middleware.multipart-params
  "Middleware that parses multipart request bodies into parameters.

  This middleware is necessary to handle file uploads from web browsers.

  Ring comes with two different multipart storage engines included:

    ring.middleware.multipart-params.byte-array/byte-array-store
    ring.middleware.multipart-params.temp-file/temp-file-store"
  (:require [ring.middleware.multipart-params.temp-file :as tf]
            [ring.util.codec :refer [assoc-conj]]
            [ring.util.request :as req]
            [ring.util.parsing :as parsing])
  (:import [org.apache.commons.fileupload2.core
            AbstractFileUpload
            RequestContext
            FileItemInput
            FileUploadException
            ProgressListener]
           [org.apache.commons.io IOUtils]))

(defn- progress-listener [request progress-fn]
  (reify ProgressListener
    (update [_ bytes-read content-length item-count]
      (progress-fn request bytes-read content-length item-count))))

(defn- set-progress-listener [^AbstractFileUpload upload request progress-fn]
  (when progress-fn
    (.setProgressListener upload (progress-listener request progress-fn))))

(defn- file-upload [request {:keys [progress-fn max-file-size]}]
  (doto (proxy [AbstractFileUpload] [])
    (.setFileSizeMax (or max-file-size -1))
    (set-progress-listener request progress-fn)))

(defn- multipart-form? [request]
  (= (req/content-type request) "multipart/form-data"))

(defn- request-context ^RequestContext [request encoding]
  (reify RequestContext
    (getContentType [_]       (get-in request [:headers "content-type"]))
    (getContentLength [_]     (or (req/content-length request) -1))
    (getCharacterEncoding [_] encoding)
    (getInputStream [_]       (:body request))
    (isMultipartRelated [_]   false)))

(defn- file-item-iterable [^AbstractFileUpload upload ^RequestContext context]
  (reify Iterable
    (iterator [_]
      (let [it (.getItemIterator upload context)]
        (reify java.util.Iterator
          (hasNext [_] (.hasNext it))
          (next [_] (.next it)))))))

(defn- parse-content-type-charset [^FileItemInput item]
  (some->> (.getContentType item) parsing/find-content-type-charset))

(defn- parse-file-item [^FileItemInput item store]
  {:field? (.isFormField item)
   :name   (.getFieldName item)
   :value  (if (.isFormField item)
             {:bytes    (IOUtils/toByteArray (.getInputStream item))
              :encoding (parse-content-type-charset item)}
             (store {:filename     (.getName item)
                     :content-type (.getContentType item)
                     :stream       (.getInputStream item)}))})

(defn- find-param [params name]
  (first (filter #(= name (:name %)) params)))

(defn- parse-html5-charset [params]
  (when-let [charset (some-> params (find-param "_charset_") :value :bytes)]
    (String. ^bytes charset "US-ASCII")))

(defn- decode-field
  [{:keys [bytes encoding]} forced-encoding fallback-encoding]
  (String. ^bytes bytes (str (or forced-encoding encoding fallback-encoding))))

(defn- build-param-map [encoding fallback-encoding params]
  (let [enc (or encoding (parse-html5-charset params))]
    (reduce (fn [m {:keys [name value field?]}]
              (assoc-conj m name (if field?
                                   (decode-field value enc fallback-encoding)
                                   value)))
            {}
            params)))

(def ^:private default-store (delay (tf/temp-file-store)))

(defn parse-multipart-params
  "Parse a multipart request map and return a map of parameters. For a list of
  available options, see: wrap-multipart-params."
  {:added "1.14"}
  ([request]
   (parse-multipart-params request {}))
  ([request options]
   (when (multipart-form? request)
     (let [store             (or (:store options) @default-store)
           max-file-count    (:max-file-count options)
           encoding          (:encoding options)
           fallback-encoding (or encoding
                                 (:fallback-encoding options)
                                 (req/character-encoding request)
                                 "UTF-8")]
       (->> (request-context request fallback-encoding)
            (file-item-iterable (file-upload request options))
            (sequence
             (map-indexed (fn [i item]
                            (if (and max-file-count (>= i max-file-count))
                              (throw (ex-info "Max file count exceeded"
                                              {:max-file-count max-file-count}))
                              (parse-file-item item store)))))
            (build-param-map encoding fallback-encoding))))))

(defn multipart-params-request
  "Adds :multipart-params and :params keys to request.
  See: wrap-multipart-params."
  {:added "1.2"}
  ([request]
   (multipart-params-request request {}))
  ([request options]
   (let [params (or (parse-multipart-params request options) {})]
     (merge-with merge request
                 {:multipart-params params}
                 {:params params}))))

(defn content-too-large-handler
  "A handler function that responds with a minimal 413 Content Too Large
  response."
  ([_]
   {:status  413
    :headers {"Content-Type" "text/plain; charset=UTF-8"}
    :body    "Uploaded content exceeded limits."})
  ([request respond _]
   (respond (content-too-large-handler request))))

(defn- handle-request-and-errors [requestf handlef errorf]
  ((try
     (let [request (requestf)]
       #(handlef request))
     (catch FileUploadException _
       errorf)
     (catch clojure.lang.ExceptionInfo _
       errorf))))

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
                       should expect a map with :filename, :content-type and
                       :stream keys, and its return value will be used as the
                       value for the parameter in the multipart parameter map.
                       The default storage function is the temp-file-store.

  :progress-fn       - a function that gets called during uploads. The
                       function should expect four parameters: request,
                       bytes-read, content-length, and item-count.

  :max-file-size     - the maximum size allowed size of a file in bytes. If
                       nil or omitted, there is no limit.

  :max-file-count    - the maximum number of files allowed in a single request.
                       If nil or omitted, there is no limit.

  :error-handler     - a handler that is invoked when the :max-file-size or
                       :max-file-count limits are exceeded. Defaults to
                       using the content-too-large-handler function."
  ([handler]
   (wrap-multipart-params handler {}))
  ([handler options]
   (let [error-handler (:error-handler options content-too-large-handler)]
     (fn
       ([request]
        (handle-request-and-errors
         #(multipart-params-request request options)
         handler
         #(error-handler request)))
       ([request respond raise]
        (handle-request-and-errors
         #(multipart-params-request request options)
         #(handler % respond raise)
         #(error-handler request respond raise)))))))

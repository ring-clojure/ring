(ns ring.middleware.multipart-params
  "Parse multipart upload into params."
  (:use [clojure.contrib.def :only (defvar-)]
        [ring.middleware.params :only (assoc-param)])
  (:import (org.apache.commons.fileupload
             FileUpload RequestContext)
           (org.apache.commons.fileupload.disk
             DiskFileItemFactory
             DiskFileItem)))

(defn- multipart-form?
  "Does a request have a multipart form?"
  [request]
  (if-let [^String content-type (:content-type request)]
    (.startsWith content-type "multipart/form-data")))

(defvar- ^FileUpload file-upload
  (FileUpload.
    (doto (DiskFileItemFactory.)
      (.setSizeThreshold -1)
      (.setFileCleaningTracker nil)))
  "Uploader class to save multipart form values to temporary files.")

(defn- request-context
  "Create a RequestContext object from a request map."
  {:tag RequestContext}
  [request encoding]
  (reify RequestContext
    (getContentType [this]       (:content-type request))
    (getContentLength [this]     (:content-length request))
    (getCharacterEncoding [this] encoding)
    (getInputStream [this]       (:body request))))

(defn- file-map
  "Create a file map from a DiskFileItem."
  [^DiskFileItem item]
  (with-meta
    {:filename     (.getName item)
     :size         (.getSize item)
     :content-type (.getContentType item)
     :tempfile     (.getStoreLocation item)}
    {:disk-file-item item}))

(defn parse-multipart-params
  "Parse a map of multipart parameters from the request."
  [request encoding]
  (reduce
    (fn [param-map, ^DiskFileItem item]
      (assoc-param param-map
        (.getFieldName item)
        (if (.isFormField item)
          (if (.get item) (.getString item))
          (file-map item))))
    {}
    (.parseRequest
       file-upload
       (request-context request encoding))))

(defn wrap-multipart-params
  "Middleware to parse multipart parameters from a request. Adds the
  following keys to the request map:
    :multipart-params - a map of multipart parameters
    :params           - a merged map of all types of parameter
  Takes an optional configuration map. Recognized keys are:
    :encoding - character encoding to use for multipart parsing. If not
                specified, uses the request character encoding, or \"UTF-8\"
                if no request character encoding is set."
  [handler & [opts]]
  (fn [request]
    (let [encoding (or (:encoding opts)
                       (:character-encoding request)
                       "UTF-8")
          params   (if (multipart-form? request)
                     (parse-multipart-params request encoding)
                     {})
          request  (merge-with merge request
                     {:multipart-params params}
                     {:params params})]
      (handler request))))

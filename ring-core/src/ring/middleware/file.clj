(ns ring.middleware.file
  "Middleware to serve files from a directory.

  Most of the time you should prefer ring.middleware.resource instead, as this
  middleware will not work with files in jar or war files."
  (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [ring.util.request :as request]
            [ring.middleware.head :as head]
            [clojure.java.io :as io]))

(defn- ensure-dir
  "Ensures that a directory exists at the given path, throwing if one does not."
  [dir-path]
  (let [dir (io/as-file dir-path)]
    (if-not (.exists dir)
      (throw (Exception. (format "Directory does not exist: %s" dir-path))))))

(defn file-request
  "If request matches a static file, returns it in a response. Otherwise
  returns nil. See: wrap-file."
  {:added "1.2"}
  ([request root-path]
   (file-request request root-path {}))
  ([request root-path options]
   (let [options (merge {:root (str root-path)
                         :index-files? true
                         :allow-symlinks? false}
                        options)]
     (if (#{:get :head} (:request-method request))
       (let [path (subs (codec/url-decode (request/path-info request)) 1)]
         (-> (response/file-response path options)
             (head/head-response request)))))))

(defn- wrap-file-prefer-files [handler root-path options]
  (fn
    ([request]
     (or (file-request request root-path options) (handler request)))
    ([request respond raise]
     (if-let [response (file-request request root-path options)]
       (respond response)
       (handler request respond raise)))))

(defn- wrap-file-prefer-handler [handler root-path options]
  (fn
    ([request]
     (let [response (handler request)]
       (if (= 404 (:status response))
         (file-request request root-path options)
         response)))
    ([request respond raise]
     (handler request
              (fn [response]
                (if (= 404 (:status response))
                  (respond (file-request request root-path options))
                  (respond response)))
              raise))))

(defn wrap-file
  "Wrap an handler such that the directory at the given root-path is checked for
  a static file with which to respond to the request, proxying the request to
  the wrapped handler if such a file does not exist.

  Accepts the following options:

  :index-files?    - look for index.* files in directories, defaults to true
  :allow-symlinks? - serve files through symbolic links, defaults to false
  :prefer-handler? - prioritize handler response over files, defaults to false"
  ([handler root-path]
   (wrap-file handler root-path {}))
  ([handler root-path options]
   (ensure-dir root-path)
   (if (:prefer-handler? options)
     (wrap-file-prefer-handler handler root-path options)
     (wrap-file-prefer-files   handler root-path options))))

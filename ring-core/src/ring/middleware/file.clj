(ns ring.middleware.file
  "Static file serving."
  (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [ring.util.request :as request]
            [ring.middleware.head :as head]
            [clojure.java.io :as io]))

(defn- ensure-dir
  "Ensures that a directory exists at the given path, throwing if one does not."
  [dir-path]
  (let [dir (io/file dir-path)]
    (if-not (.exists dir)
      (throw (Exception. (format "Directory does not exist: %s" dir-path))))))

(defn file-request
  "If request matches a static file, returns it in a response. Otherwise returns nil."
  [req root-path & [opts]]
  (let [opts (merge {:root (str root-path), :index-files? true, :allow-symlinks? false} opts)]
    (if (= :get (:request-method req))
      (let [path (subs (codec/url-decode (request/path-info req)) 1)]
        (response/file-response path opts)))))

(defn wrap-file
  "Wrap an handler such that the directory at the given root-path is checked for
  a static file with which to respond to the request, proxying the request to
  the wrapped handler if such a file does not exist.

  An map of options may be optionally specified. These options will be passed
  to the ring.util.response/file-response function."
  [handler root-path & [opts]]
  (ensure-dir root-path)
  (fn [req]
    (or ((head/wrap-head #(file-request % root-path opts)) req)
        (handler req))))

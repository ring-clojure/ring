(ns ring.middleware.file
  "Static file serving."
  (:require [ring.util.codec :as codec]
            [ring.util.response :as response])
  (:import java.io.File))

(defn- ensure-dir
  "Ensures that a directory exists at the given path, throwing if one does not."
  [^String dir-path]
  (let [dir (File. dir-path)]
    (if-not (.exists dir)
      (throw (Exception. (format "Directory does not exist: %s" dir-path))))))

(defn wrap-file
  "Wrap an app such that the directory at the given root-path is checked for a
  static file with which to respond to the request, proxying the request to the
  wrapped app if such a file does not exist.

  An map of options may be optionally specified. These options will be passed
  to the ring.util.response/file-response function."
  [app ^String root-path & [opts]]
  (ensure-dir root-path)
  (let [opts (merge {:root root-path, :index-files? true} opts)]
    (fn [req]
      (if-not (= :get (:request-method req))
        (app req)
        (let [path (.substring ^String (codec/url-decode (:uri req)) 1)]
          (or (response/file-response path opts)
              (app req)))))))

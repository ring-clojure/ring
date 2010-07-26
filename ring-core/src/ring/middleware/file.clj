(ns ring.middleware.file
  "Static file serving."
  (:use (clojure.contrib [def :only (defvar-)]
                         [except :only (throw-if-not)]))
  (:require (ring.util [codec :as codec]
                       [response :as response]))
  (:import java.io.File))

(defn- ensure-dir
  "Ensures that a directory exists at the given path, throwing if one does not."
  [^String dir-path]
  (let [dir (File. dir-path)]
    (throw-if-not (.exists dir) "Directory does not exist: %s" dir-path)))

(defn wrap-file
  "Wrap an app such that the directory at the given root-path is checked for a
  static file with which to respond to the request, proxying the request to the
  wrapped app if such a file does not exist."
  [app ^String root-path]
  (ensure-dir root-path)
  (fn [req]
    (if-not (= :get (:request-method req))
      (app req)
      (let [path (.substring (codec/url-decode (:uri req)) 1)]
        (or
          (response/file-response path
            {:root root-path :index-files? true :html-files? true})
          (app req))))))

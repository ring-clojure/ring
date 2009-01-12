(ns ring.file
  (:use clojure.contrib.except
        ring.utils)
  (:import (java.io File)))

(defn- ensure-dir
  "Ensures that the given directory exists."
  [dir]
  (throw-if-not (.exists dir)
    "Directory does not exist: %s" dir))

(defn- forbidden
  []
  {:status  403
   :headers {"Content-Type" "text/html"}
   :body    "<html><body><h1>403 Forbidden</h1></body></html>"})

(defn- success
  [file]
  {:status 200 :headers {} :body file})

(defn- maybe-file
  "Returns the File corresponding to the given relative path within the given
  dir if it exists, or nil if no such file exists."
  [dir path]
  (let [file (File. dir path)]
    (and (.exists file) (.canRead file) file)))

(defn wrap
  "Wrap an app such that a given directory is checked for a static file
  with which to respond to the request, proxying the request to the
  wrapped app if such a file does not exist."
  ([dir app]
   (ensure-dir dir)
   (fn [req]
     (if (#{:get :head} (:request-method req))
       (let [uri (url-decode (:uri req))]
         (if (str-includes? ".." uri)
           (forbidden)
           (let [path (cond
                        (.endsWith "/" uri) (str uri "index.html")
                        (re-match? #"\.[a-z]+$" uri) uri
                        :else (str uri ".html"))]
             (if-let [file (maybe-file dir path)]
               (success file)
               (app req)))))
       (app req))))
  ([dir]
   (partial wrap dir)))

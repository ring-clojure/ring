(ns ring.middleware.file
  (:use clojure.contrib.except)
  (:import (java.io File)))

(defn- url-decode
  "Returns the form-url-decoded version of the given string."
  [encoded]
  (java.net.URLDecoder/decode encoded "UTF-8"))

(defn- str-includes?
  "Returns logical truth iff the given target appears in the given string"
  [#^String target #^String string]
  (<= 0 (.indexOf string target)))

(defn- ensure-dir
  "Ensures that the given directory exists and returns it, throwing if it does
  not. If the directory is given as a String, it is coerced to a Fil before
  returning."
  [dir]
  (let [#^File fdir (if (string? dir) (File. #^String dir) dir)]
    (throw-if-not (.exists fdir)
      "Directory does not exist: %s" fdir)
    fdir))

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
  [#^File dir #^String path]
  (let [file (File. dir path)]
    (and (.exists file) (.canRead file) file)))

(defn wrap-file
  "Wrap an app such that a given directory is checked for a static file
  with which to respond to the request, proxying the request to the
  wrapped app if such a file does not exist."
  [app dir]
  (let [fdir (ensure-dir dir)]
    (fn [req]
      (if (#{:get :head} (:request-method req))
        (let [uri (url-decode (:uri req))]
          (if (str-includes? ".." uri)
            (forbidden)
            (let [path (cond
                         (.endsWith "/" uri)        (str uri "index.html")
                         (re-find #"\.[a-z]+$" uri) uri
                         :else                      (str uri ".html"))]
              (if-let [file (maybe-file fdir path)]
                (success file)
                (app req)))))
        (app req)))))

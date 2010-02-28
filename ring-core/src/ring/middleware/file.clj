(ns ring.middleware.file
  "Static file serving."
  (:use (clojure.contrib [def :only (defvar-)]
                         [except :only (throw-if-not)])
        ring.util.response)
  (:require [ring.util.codec :as codec]
            [clojure.contrib.java-utils :as ju])
  (:import java.io.File))

(defn- substring?
  "Returns true if sub is a substring of string."
  [sub #^String string]
  (.contains string sub))

(defn- ensure-dir
  "Ensures that a given directory exists, throwing if it does not."
  [dir]
  (let [fdir (ju/file dir)]
    (throw-if-not (.exists fdir) "Directory does not exist: %s" fdir)))

(defvar- forbidden
  (-> (response "<html><body><h1>403 Forbidden</h1></body></html>")
    (status 403)
    (content-type "text/html")))

(defn- maybe-file
  "Returns the File corresponding to the given relative path within the given
  dir if it exists, or nil if no such file exists."
  [dir path]
  (let [file (ju/file (str dir path))]
    (and (.exists file) (.canRead file) file)))

(defn wrap-file
  "Wrap an app such that a given directory is checked for a static file
  with which to respond to the request, proxying the request to the
  wrapped app if such a file does not exist."
  [app dir]
  (ensure-dir dir)
  (fn [req]
    (if (#{:get :head} (:request-method req))
      (let [uri (codec/url-decode (:uri req))]
        (if (substring? ".." uri)
          forbidden
          (let [path (cond
                       (.endsWith "/" uri)        (str uri "index.html")
                       (re-find #"\.[a-z]+$" uri) uri
                       :else                      (str uri ".html"))]
            (if-let [file (maybe-file dir path)]
              (response file)
              (app req)))))
      (app req))))

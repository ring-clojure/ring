(ns ring.util.response
  "Generate and augment Ring responses."
  (:import java.io.File java.util.Date java.net.URL
           java.net.URLDecoder java.net.URLEncoder)
  (:use [ring.util.time :only (format-date)]
        [ring.util.io :only (last-modified-date)])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn redirect
  "Returns a Ring response for an HTTP 302 redirect."
  [url]
  {:status  302
   :headers {"Location" url}
   :body    ""})

(defn redirect-after-post
  "Returns a Ring response for an HTTP 303 redirect."
  [url]
  {:status  303
   :headers {"Location" url}
   :body    ""})

(defn created
  "Returns a Ring response for a HTTP 201 created response."
  ([url] (created url nil))
  ([url body]
     {:status  201
      :headers {"Location" url}
      :body    body}))

(defn not-found
  "Returns a 404 'not found' response."
  [body]
  {:status  404
   :headers {}
   :body    body})

(defn response
  "Returns a skeletal Ring response with the given body, status of 200, and no
  headers."
  [body]
  {:status  200
   :headers {}
   :body    body})

(defn status
  "Returns an updated Ring response with the given status."
  [resp status]
  (assoc resp :status status))

(defn header
  "Returns an updated Ring response with the specified header added."
  [resp name value]
  (assoc-in resp [:headers name] (str value)))

(defn- safe-path?
  "Is a filepath safe for a particular root?"
  [^String root ^String path]
  (.startsWith (.getCanonicalPath (File. root path))
               (.getCanonicalPath (File. root))))

(defn- directory-transversal?
  "Check if a path contains '..'."
  [^String path]
  (-> (str/split path #"/|\\")
      (set)
      (contains? "..")))

(defn- find-index-file
  "Search the directory for an index file."
  [^File dir]
  (first
    (filter
      #(.startsWith (.toLowerCase (.getName ^File %)) "index.")
       (.listFiles dir))))

(defn- safely-find-file [^String path opts]
  (if-let [^String root (:root opts)]
    (if (or (safe-path? root path)
            (and (:allow-symlinks? opts) (not (directory-transversal? path))))
      (File. root path))
    (File. path)))

(defn- find-file [^String path opts]
  (if-let [^File file (safely-find-file path opts)]
    (cond
      (.isDirectory file)
        (and (:index-files? opts true) (find-index-file file))
      (.exists file)
        file)))

(defn- file-content-length [resp]
  (let [file ^File (:body resp)]
    (header resp "Content-Length" (.length file))))

(defn- file-last-modified [resp]
  (let [file ^File (:body resp)]
    (header resp "Last-Modified" (format-date (last-modified-date file)))))

(defn file-response
  "Returns a Ring response to serve a static file, or nil if an appropriate
  file does not exist.
  Options:
    :root            - take the filepath relative to this root path
    :index-files?    - look for index.* files in directories, defaults to true
    :allow-symlinks? - serve files through symbolic links, defaults to false"
  [filepath & [opts]]
  (if-let [file (find-file filepath opts)]
    (-> (response file)
        (file-content-length)
        (file-last-modified))))

;; In Clojure versions 1.2.0, 1.2.1 and 1.3.0, the as-file function
;; in clojure.java.io does not correctly decode special characters in
;; URLs (e.g. '%20' should be turned into ' ').
;;
;; See: http://dev.clojure.org/jira/browse/CLJ-885
;;
;; In Clojure 1.5.1, the as-file function does not correctly decode
;; UTF-8 byte sequences.
;;
;; See: http://dev.clojure.org/jira/browse/CLJ-1177
;;
;; As a work-around, we'll backport the fix from CLJ-1177 into
;; url-as-file.

(defn- ^File url-as-file [^java.net.URL u]
  (-> (.getFile u)
      (str/replace \/ File/separatorChar)
      (str/replace "+" (URLEncoder/encode "+" "UTF-8"))
      (URLDecoder/decode "UTF-8")
      io/as-file))

(defn content-type
  "Returns an updated Ring response with the a Content-Type header corresponding
  to the given content-type."
  [resp content-type]
  (header resp "Content-Type" content-type))

(defn charset
  "Returns an updated Ring response with the supplied charset added to the
  Content-Type header."
  [resp charset]
  (update-in resp [:headers "Content-Type"]
    (fn [content-type]
      (-> (or content-type "text/plain")
          (str/replace #";\s*charset=[^;]*" "")
          (str "; charset=" charset)))))

(defn set-cookie
  "Sets a cookie on the response. Requires the handler to be wrapped in the
  wrap-cookies middleware."
  [resp name value & [opts]]
  (assoc-in resp [:cookies name] (merge {:value value} opts)))

(defn response?
  "True if the supplied value is a valid response map."
  [resp]
  (and (map? resp)
       (integer? (:status resp))
       (map? (:headers resp))))

(defn- connection-content-length [resp ^java.net.URLConnection conn]
  (let [content-length (.getContentLength conn)]
    (if (neg? content-length)
      resp
      (header resp "Content-Length" content-length))))

(defn- connection-last-modified [resp ^java.net.URLConnection conn]
  (let [last-modified (.getLastModified conn)]
    (if (zero? last-modified)
      resp
      (header resp "Last-Modified" (format-date (Date. last-modified))))))

(defn- file-url [^java.net.URL url]
  (if (= "file" (.getProtocol url))
    (url-as-file url)))

(defn url-response
  "Return a response for the supplied URL."
  [^URL url]
  (if-let [^File file (file-url url)]
    (if-not (.isDirectory file)
      (-> (response file)
          (file-content-length)
          (file-last-modified)))
    (let [conn (.openConnection url)]
      (if-let [stream (.getInputStream conn)]
        (-> (response stream)
            (connection-content-length conn)
            (connection-last-modified conn))))))

(defn resource-response
  "Returns a Ring response to serve a packaged resource, or nil if the
  resource does not exist.
  Options:
    :root - take the resource relative to this root"
  [path & [opts]]
  (let [path (-> (str (:root opts "") "/" path)
                 (.replace "//" "/")
                 (.replaceAll "^/" ""))]
    (if-let [resource (io/resource path)]
      (url-response resource))))

(defn get-header
  "Look up a header in a Ring response (or request) case insensitively,
  returning the value of the header."
  [resp ^String header-name]
  (some (fn [[k v]] (if (.equalsIgnoreCase header-name k) v))
        (:headers resp)))

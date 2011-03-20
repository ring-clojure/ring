(ns ring.util.response
  "Generate and augment Ring responses."
  (:import java.io.File)
  (:require [clojure.java.io :as io]))

(defn redirect
  "Returns a Ring response for an HTTP 302 redirect."
  [url]
  {:status  302
   :headers {"Location" url}
   :body    ""})

(defn response
  "Returns a skeletal Ring response with the given body, status of 200, and no
  headers."
  [body]
  {:status  200
   :headers {}
   :body    body})

(defn- safe-path?
  "Is a filepath safe for a particular root?"
  [^String root ^String path]
  (.startsWith (.getCanonicalPath (File. root path))
               (.getCanonicalPath (File. root))))

(defn- find-index-file
  "Search the directory for an index file."
  [^File dir]
  (first
    (filter
      #(.startsWith (.toLowerCase (.getName ^File %)) "index.")
       (.listFiles dir))))

(defn- get-file
  "Safely retrieve the correct file. See file-response for an
  explanation of options."
  [^String path opts]
  (if-let [file (if-let [^String root (:root opts)]
                  (and (safe-path? root path) (File. root path))
                  (File. path))]
    (cond
      (.isDirectory file)
        (and (:index-files? opts true) (find-index-file file))
      (.exists file)
        file)))

(defn file-response
  "Returns a Ring response to serve a static file, or nil if an appropriate
  file does not exist.
  Options:
    :root         - take the filepath relative to this root path
    :index-files? - look for index.* files in directories, defaults to true"
  [filepath & [opts]]
  (if-let [file (get-file filepath opts)]
    (response file)))

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
      (response
       (if (= "file" (.getProtocol resource))
         (io/as-file resource)
         (io/input-stream resource))))))

(defn status
  "Returns an updated Ring response with the given status."
  [resp status]
  (assoc resp :status status))

(defn header
  "Returns an updated Ring response with the specified header added."
  [resp name value]
  (assoc-in resp [:headers name] (str value)))

(defn content-type
  "Returns an updated Ring response with the a Content-Type header corresponding
  to the given content-type."
  [resp content-type]
  (header resp "Content-Type" content-type))

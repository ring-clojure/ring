(ns ring.util.response
  (:import java.io.File))

(defn redirect-to
  "Returns a Ring response for an HTTP redirect. 
  Options:
    :status, defaults to 302"
  [url & [opts]]
  {:status  (:status opts 302)
   :headers {"Location" url}
   :body    ""})

(defn- safe-path?
  "Is a filepath safe for a particular root?"
  [root path]
  (.startsWith (.getCanonicalPath (File. root path))
               (.getCanonicalPath (File. root))))

(defn- find-index-file
  "Search the directory for an index file."
  [dir]
  (first
    (filter
      #(.startsWith (.toLowerCase (.getName %)) "index.")
       (.listFiles dir))))

(defn- get-file
  "Safely retrieve the correct file. See serve-file for an explanation of
  options."
  [path opts]
  (let [file (if-let [root (:root opts)]
               (if (safe-path? root path)
                 (File. root path))
               (File. path))]
    (if (.exists file)
      (if (.isDirectory file)
        (if (:index-files? opts true)
          (find-index-file file))
        file))))

(defn static-file
  "Returns a Ring response to serve a static file, or nil if the file does
  not exist.
  Options:
    :root         - take the filepath relative to this root path
    :index-files? - look for index.* files in directories, defaults to true"
  [filepath & [opts]]
  (if-let [file (get-file filepath opts)]
    {:status  200
     :headers {}
     :body    file}))

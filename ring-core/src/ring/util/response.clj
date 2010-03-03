(ns ring.util.response
  "Generate and augment Ring responses."
  (:import java.io.File))

; Ring responses

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
   :body    body
   :headers {}})

(defn- safe-path?
  "Is a filepath safe for a particular root?"
  [#^String root #^String path]
  (.startsWith (.getCanonicalPath (File. root path))
               (.getCanonicalPath (File. root))))

(defn- find-index-file
  "Search the directory for an index file."
  [#^File dir]
  (first
    (filter
      #(.startsWith (.toLowerCase (.getName #^File %)) "index.")
       (.listFiles dir))))

(defn- get-file
  "Safely retrieve the correct file. See static-file for an
  explanation of options."
  [#^String path opts]
  (let [file (if-let [#^String root (:root opts)]
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
    (response file)))

; Ring response augmenters

(defn status
  "Returns an updated Ring response with the given status."
  [resp status]
  (assoc resp :status status))

(defn content-type
  "Returns an updated Ring response with the a Content-Type header corresponding
  to the given content-type."
  [resp content-type]
  (assoc-in resp [:headers "Content-Type"] content-type))

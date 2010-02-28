(ns ring.util.file
  (:import java.io.File))

(defn safe-path?
  "Is a filepath safe for a particular root?"
  [root path]
  (.startsWith (.getCanonicalPath (File. root path))
               (.getCanonicalPath (File. root))))

(defn find-index-file
  "Search the directory for an index file."
  [dir]
  (first
    (filter
      #(.startsWith (.toLowerCase (.getName %)) "index.")
       (.listFiles dir))))

(defn get-file
  "Safely retrieve the correct file. See ring.util.response/static-file for an
  explanation of options."
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

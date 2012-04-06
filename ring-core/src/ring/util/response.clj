(ns ring.util.response
  "Generate and augment Ring responses."
  (:import java.io.File)
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
  (if-let [^File file (if-let [^String root (:root opts)]
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

;; In Clojure versions 1.2.0, 1.2.1 and 1.3.0, the as-file function
;; in clojure.java.io does not correctly decode special characters in
;; URLs (e.g. '%20' should be turned into ' ').
;;
;; See: http://dev.clojure.org/jira/browse/CLJ-885
;;
;; As a work-around, we'll backport the fix from the Clojure master
;; branch into url-as-file.

(defn- url-as-file [u]
  (io/as-file
   (str/replace
    (.replace (.getFile u) \/ File/separatorChar)
    #"%.."
    (fn [escape]
      (-> escape
          (.substring 1 3)
          (Integer/parseInt 16)
          (char)
          (str))))))

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
      (if (= "file" (.getProtocol resource))
        (let [file (url-as-file resource)]
          (if-not (.isDirectory file)
            (response file)))
        (response (io/input-stream resource))))))

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

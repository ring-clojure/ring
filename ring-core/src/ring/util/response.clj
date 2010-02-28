(ns ring.util.response
  (:require (ring.util [file :as file])))

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

(defn static-file
  "Returns a Ring response to serve a static file, or nil if the file does
  not exist.
  Options:
    :root         - take the filepath relative to this root path
    :index-files? - look for index.* files in directories, defaults to true"
  [filepath & [opts]]
  (if-let [file (file/get-file filepath opts)]
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

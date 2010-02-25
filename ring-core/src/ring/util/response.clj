(ns ring.util.response)

(defn redirect-to
  "Returns a Ring response for an HTTP redirect. 
  Options:
    :status, defaults to 302"
  [url & [opts]]
  {:status  (:status opts 302)
   :headers {"Location" url}
   :body    ""})

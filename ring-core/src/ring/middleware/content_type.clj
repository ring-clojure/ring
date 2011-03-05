(ns ring.middleware.content-type
  "Middleware for automatically adding a content type to response maps."
  (:use ring.util.mime-type
        ring.util.response))

(defn wrap-content-type
  "Middleware that adds a content-type header to the response if one is not
  set by the handler. Uses the ring.util.mime-type/ext-mime-type function to
  guess the content-type from the file extension in the URI. If no
  content-type can be found, it defaults to 'application/octet-stream'.

  Accepts the following options:
    :mime-types - a map of filename extensions to mime-types that will be
                  used in addition to the ones defined in
                  ring.util.mime-types/default-mime-types"
  [handler & [opts]]
  (fn [req]
    (if-let [resp (handler req)]
      (if (get-in resp [:headers "Content-Type"])
        resp
        (let [mime-type (ext-mime-type (:uri req) (:mime-types opts))]
          (content-type resp (or mime-type "application/octet-stream")))))))

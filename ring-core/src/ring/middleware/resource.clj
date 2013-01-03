(ns ring.middleware.resource
  "Middleware for serving static resources."
  (require [ring.util.codec :as codec]
           [ring.util.response :as response]))

(defn wrap-resource
  "Middleware that first checks to see whether the request map matches a static
  resource. If it does, the resource is returned in a response map, otherwise
  the request map is passed onto the handler. The root-path argument will be
  added to the beginning of the resource path. Also supports optional parameter
  to use a key other than :uri, for example :path-info for immutant support."
  ([handler root-path]
     (wrap-resource handler root-path :uri))
  ([handler root-path path-key]
     (fn [request]
       (if-not (= :get (:request-method request))
         (handler request)
         (let [path (.substring (codec/url-decode (path-key request)) 1)]
           (or (response/resource-response path {:root root-path})
               (handler request)))))))

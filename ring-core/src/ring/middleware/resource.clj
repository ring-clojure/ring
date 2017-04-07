(ns ring.middleware.resource
  "Middleware for serving static resources."
  (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [ring.util.request :as request]
            [ring.middleware.head :as head]))

(defn resource-request
  "If request matches a static resource, returns it in a response map.
  Otherwise returns nil. See wrap-resource for the available options."
  {:added "1.2"}
  ([request root-path]
   (resource-request request root-path {}))
  ([request root-path options]
   (if (#{:head :get} (:request-method request))
     (let [path (subs (codec/url-decode (request/path-info request)) 1)]
       (-> (response/resource-response path (assoc options :root root-path))
           (head/head-response request))))))

(defn wrap-resource
  "Middleware that first checks to see whether the request map matches a static
  resource. If it does, the resource is returned in a response map, otherwise
  the request map is passed onto the handler. The root-path argument will be
  added to the beginning of the resource path.

  Accepts the following options:

  :loader          - resolve the resource using this class loader
  :allow-symlinks? - allow symlinks that lead to paths outside the root
                     classpath directories (defaults to false)"
  ([handler root-path]
   (wrap-resource handler root-path {}))
  ([handler root-path options]
   (fn
     ([request]
      (or (resource-request request root-path options)
          (handler request)))
     ([request respond raise]
      (if-let [response (resource-request request root-path options)]
        (respond response)
        (handler request respond raise))))))

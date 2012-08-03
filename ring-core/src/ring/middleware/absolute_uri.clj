(ns ring.middleware.absolute-uri
  "Middleware for adding absolute URI of request to request map.")

(defn- absolute-uri [req]
  (let [scheme (-> req :scheme name)
        host (:server-name req)
        port (:server-port req)
        port-part (if (= port 80) "" (str ":" port))
        path (:uri req)
        query (when-let [q (:query-string req)] (str "?" q))]
    (str scheme "://" host port-part path query)))

(defn wrap-absolute-uri [handler]
  (fn [request]
    (handler (assoc request :absolute-uri (absolute-uri request)))))


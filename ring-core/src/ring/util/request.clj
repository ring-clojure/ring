(ns ring.util.request
  "Functions that operate on Ring requests")

(defn request-url [req]
  (let [scheme (-> req :scheme name)
        host (:server-name req)
        port (:server-port req)
        port-part (if (= port 80) "" (str ":" port))
        path (:uri req)
        query (when-let [q (:query-string req)] (str "?" q))]
    (str scheme "://" host port-part path query)))

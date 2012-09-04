(ns ring.util.request
  "Functions that operate on Ring requests"
  (:require [clojure.string :as str]))

(defn base-url [req]
  "Returns a request's base URL, e.g. http://example.com/"
  (let [port (:server-port req)]
    (str (-> req :scheme name)
         "://"
         (:server-name req)
         (when-not (= port 80) (str ":" port))
         "/")))

(defn request-url [req]
  "Returns a request's URL, e.g. http://example.com/a/b/c?d=f"
  (str (-> req base-url (str/replace-first #"/$" ""))
       (:uri req)
       (when-let [q (:query-string req)] (str "?" q))))

(ns ring.util.request
  "Functions that operate on Ring requests"
  (:require [clojure.string :as str]))

(defn base-url [req]
  (let [port (:server-port req)]
    (str (-> req :scheme name)
         "://"
         (:server-name req)
         (when-not (= port 80) (str ":" port))
         "/")))

(defn request-url [req]
  (str (-> req base-url (str/replace-first #"/$" ""))
       (:uri req)
       (when-let [q (:query-string req)] (str "?" q))))

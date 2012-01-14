(ns ring.util.request
  "Parse and interpret Ring requests"
  (:require [clojure.string :as str]
            [clojure.walk :as walk]))

(defn header-seq
  "Returns seq of values for the specified header. Multiple values can be
  encoded in one header key using a comma as a separator."
  [request key]
  (if-let [value (get-in request [:headers (name key)])]
    (str/split value #"\s*,\s*")))

(defn- parse-attr-map [attrs]
  (->> (map #(str/split % #"=") attrs)
       (into {})
       (walk/keywordize-keys)))

(defn- parse-accept-value [value]
  (let [[value & attrs] (str/split value #"\s*;\s*")]
    (-> (parse-attr-map attrs)
        (update-in [:q] #(Double. (or % "1.0")))
        (assoc :value value))))

(defn accept
  "Parse the accept header and returns an ordered seq of maps for each media
  type. Each map will contain at least a :value and a :q key.

  e.g. (accept {:headers {\"accept\": \"text/html, text/xml;q=0.8\"})

       => ({:value \"text/html\", :q 1.0}
           {:value \"text/xml\",  :q 0.8})"
  [request]
  (->> (header-seq request "accept")
       (map parse-accept-value)))

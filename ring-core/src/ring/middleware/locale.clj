(ns ^{:doc "This namespace contains middleware that digests locale
  information and adds it to the request map."}
  ring.middleware.locale)


(defn- acceptable-locale? [possible-locale accepted-locales]
  (or (contains? accepted-locales possible-locale) (empty? accepted-locales)))

(defn- has-accepted-locale? [request options]
  (if-let [locale (:locale request)]
    (acceptable-locale? locale (:accepted-locales options))
    false))

(defn wrap-locale [handler & options]
  (let [options (apply hash-map options)]
    (fn [request]
      (let [augmented-requests (map #(% request options) (:locale-augmenters options))
            requests-with-accepted-locale-data (filter #(has-accepted-locale? % options) augmented-requests)]
        (handler (or
          (first requests-with-accepted-locale-data)
          (assoc request :locale (:default-locale options))))))))
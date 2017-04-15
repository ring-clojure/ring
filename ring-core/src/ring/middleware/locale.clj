(ns ^{:doc "This namespace contains middleware that digests locale
  information and adds it to the request map."}
  ring.middleware.locale
  (:use
    [clojure.string :as string :only [split join]]
    [ring.middleware.cookies :only [parse-cookies]]
    [ring.middleware.params :only [parse-params]]))


(defn- scrubbed-uri [uri]
  (str "/" (string/join "/" (nnext (string/split uri #"\/")))))

(defn- acceptable-locale? [possible-locale accepted-locales]
  (or (contains? accepted-locales possible-locale) (empty? accepted-locales)))

(defn- has-accepted-locale? [request options]
  (if-let [locale (:locale request)]
    (acceptable-locale? locale (:accepted-locales options))
    false))

(defn- split-locale-and-q [locale-and-q-score]
  (string/split locale-and-q-score #";"))

(defn- add-implicit-q-score [locale-q-pair]
  (if (= 1 (count locale-q-pair))
    (conj locale-q-pair "q=1")
    locale-q-pair))

(defn- accepted-locale-and-q-pairs [accept-header options]
  (let [locale-and-q-scores-list (string/split accept-header #",")
        locale-and-q-pairs (map add-implicit-q-score (map split-locale-and-q locale-and-q-scores-list))]
    (filter #(acceptable-locale? (first %) (:accepted-locales options)) locale-and-q-pairs)))


(defn uri [request options]
  (let [locale (second (string/split (:uri request) #"\/"))]
    (assoc request :locale locale :uri (scrubbed-uri (:uri request)))))

(defn cookie [request options]
  (let [cookies (or (:cookies request) (parse-cookies request))
        locale-cookie-name (or (:locale-cookie-name options) "locale")]
    (assoc request :locale (:value (get cookies locale-cookie-name)))))

(defn query-param [request options]
  (let [query-params (or (:query-params request) (parse-params (:query-string request) "UTF-8"))
        locale-param-name (or (:locale-param-name options) "locale")]
    (assoc request :locale (get query-params locale-param-name))))

(defn accept-header [request options]
  (let [accept-header (or (get (:headers request) "accept-language") "")
        sorted-locale-and-q-pairs (sort #(compare (last %2) (last %1)) (accepted-locale-and-q-pairs accept-header options))
        locale (ffirst sorted-locale-and-q-pairs)]
    (assoc request :locale (if (not (= "" locale)) locale))))


(defn wrap-locale [handler & options]
  (let [options (apply hash-map options)]
    (fn [request]
      (let [augmented-requests (map #(% request options) (:locale-augmenters options))
            requests-with-accepted-locale-data (filter #(has-accepted-locale? % options) augmented-requests)]
        (handler (or
          (first requests-with-accepted-locale-data)
          (assoc request :locale (:default-locale options))))))))

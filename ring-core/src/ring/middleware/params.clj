(ns ring.middleware.params
  (:use (clojure.contrib def duck-streams str-utils))
  (:require (ring.util [codec :as codec])))

(defn assoc-param
  "Associate a key with a value. If the key already exists in the map,
  create a vector of values."
  [map key val]
  (assoc map key
    (if-let [cur (map key)]
      (if (vector? cur)
        (conj cur val)
        [cur val])
      val)))

(defn- parse-params
  "Parse parameters from a string into a map."
  [#^String param-string encoding]
  (reduce
    (fn [param-map encoded-param]
      (if-let [[_ key val] (re-matches #"([^=]+)=(.*)" encoded-param)]
        (assoc-param param-map
          (codec/url-decode key encoding)
          (codec/url-decode (or val "") encoding))
         param-map))
    {}
    (re-split #"&" param-string)))

(defn- assoc-query-params
  "Parse and assoc parameters from the query string with the request."
  [request encoding]
  (merge-with merge request
    (if-let [query-string (:query-string request)]
      (let [params (parse-params query-string encoding)]
        {:query-params params, :params params})
      {:query-params {}, :params {}})))

(defn- urlencoded-form?
  "Does a request have a urlencoded form?"
  [request]
  (if-let [#^String type (:content-type request)]
    (.startsWith type "application/x-www-form-urlencoded")))

(defn- assoc-form-params
  "Parse and assoc parameters from the request body with the request."
  [request encoding]
  (merge-with merge request
    (if-let [body (and (urlencoded-form? request) (:body request))]
      (let [params (parse-params (slurp* body) encoding)]
        {:form-params params, :params params})
      {:form-params {}, :params {}})))

(defn wrap-params
  "Middleware to parse urlencoded parameters from the query string and form
  body (if the request is a urlencoded form). Adds the following keys to
  the request map:
    :query-params - a map of parameters from the query string
    :form-params  - a map of parameters from the body
    :params       - a merged map of all types of parameter
  Takes an optional configuration map. Recognized keys are:
    :encoding - encoding to use for url-decoding. If not specified, uses
                the request character encoding, or \"UTF-8\" if no request
                character encoding is set."
  [handler & [opts]]
  (fn [request]
    (let [encoding (or (:encoding opts)
                       (:character-encoding request)
                       "UTF-8")
          request  (if (:form-params request)
                     request
                     (assoc-form-params request encoding))
          request  (if (:query-params request)
                     request
                     (assoc-query-params request encoding))]
      (handler request))))

(ns ring.middleware.params
  (:use clojure.contrib.def)
  (:use clojure.contrib.duck-streams)
  (:use clojure.contrib.str-utils)
  (:import java.net.URLDecoder))

(defvar *encoding* "UTF-8"
  "The encoding to use to decode urlencoded parameters. Defaults to
  UTF-8.")

(defn- urldecode
  "Decode a urlencoded string using the *encoding* var."
  [s]
  (URLDecoder/decode s *encoding*))

(defn- assoc-vec
  "Associate a key with a value. If the key already exists in the map,
  create a vector of values."
  [map key val]
  (assoc map key
    (if-let [cur (map key)]
      (if (vector? cur)
        (conj cur val)
        [cur val])
      val)))

(defn parse-params
  "Parse parameters from a string into a map."
  [#^String param-string]
  (reduce
    (fn [param-map encoded-param]
      (if-let [[_ key val] (re-matches #"([^=]+)=(.*)" encoded-param)]
        (assoc-vec param-map
          (urldecode key)
          (urldecode (or val "")))
         param-map))
    {}
    (re-split #"&" param-string)))

(defn- assoc-query-params
  "Parse and assoc parameters from the query string with the request."
  [request]
  (if-let [query-string (:query-string request)]
    (let [params (parse-params query-string)]
      (merge-with merge request {:query-params params, :params params}))
    request))

(defn- urlencoded-form?
  "Does a request have a urlencoded form?"
  [request]
  (if-let [type (:content-type request)]
    (.startsWith type "application/x-www-form-urlencoded")))

(defn- assoc-form-params
  "Parse and assoc parameters from the request body with the request."
  [request]
  (if-let [body (and (urlencoded-form? request) (:body request))]
    (let [params (parse-params (slurp* body))]
      (merge-with merge request {:form-params params, :params params}))
    request))

(defn wrap-params
  "Middleware to parse urlencoded parameters from the query string and form
  body (if the request is a urlencoded form). Adds the following keys to
  the request map:
    :query-params - a map of parameters from the query string
    :form-params  - a map of parameters from the body
    :params       - a merged map of both query and form parameters"
  [handler]
  (fn [request]
    (-> request
      assoc-form-params
      assoc-query-params
      handler)))

(ns ring.middleware.params
  "Parse form and query params."
  (:require [ring.util.codec :as codec]))

(defn parse-params [params encoding]
  (let [params (codec/form-decode params encoding)]
    (if (map? params) params {})))

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
  (if-let [^String type (:content-type request)]
    (.startsWith type "application/x-www-form-urlencoded")))

(defn- assoc-form-params
  "Parse and assoc parameters from the request body with the request."
  [request encoding]
  (merge-with merge request
    (if-let [body (and (urlencoded-form? request) (:body request))]
      (let [params (parse-params (slurp body :encoding encoding) encoding)]
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

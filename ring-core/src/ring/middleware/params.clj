(ns ring.middleware.params
  "Middleware to parse url-encoded parameters from the query string and request
  body."
  (:require [ring.util.codec :as codec]
            [ring.util.request :as req]))

(defn- parse-params [params encoding]
  (let [params (codec/form-decode params encoding)]
    (if (map? params) params {})))

(defn assoc-query-params
  "Parse and assoc parameters from the query string with the request."
  {:added "1.3"}
  [request encoding]
  (merge-with merge request
    (if-let [query-string (:query-string request)]
      (let [params (parse-params query-string encoding)]
        {:query-params params, :params params})
      {:query-params {}, :params {}})))

(defn assoc-form-params
  "Parse and assoc parameters from the request body with the request."
  {:added "1.2"}
  [request encoding]
  (merge-with merge request
    (if-let [body (and (req/urlencoded-form? request) (:body request))]
      (let [params (parse-params (slurp body :encoding encoding) encoding)]
        {:form-params params, :params params})
      {:form-params {}, :params {}})))

(defn params-request
  "Adds parameters from the query string and the request body to the request
  map. See: wrap-params."
  {:added "1.2"}
  ([request]
   (params-request request {}))
  ([request options]
   (let [encoding (or (:encoding options)
                      (req/character-encoding request)
                      "UTF-8")
         request  (if (:form-params request)
                    request
                    (assoc-form-params request encoding))]
     (if (:query-params request)
       request
       (assoc-query-params request encoding)))))

(defn wrap-params
  "Middleware to parse urlencoded parameters from the query string and form
  body (if the request is a url-encoded form). Adds the following keys to
  the request map:

  :query-params - a map of parameters from the query string
  :form-params  - a map of parameters from the body
  :params       - a merged map of all types of parameter

  Accepts the following options:      

  :encoding - encoding to use for url-decoding. If not specified, uses
              the request character encoding, or \"UTF-8\" if no request
              character encoding is set."
  ([handler]
   (wrap-params handler {}))
  ([handler options]
   (fn
     ([request]
      (handler (params-request request options)))
     ([request respond raise]
      (handler (params-request request options) respond raise)))))

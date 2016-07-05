(ns ring.middleware.not-modified
  "Middleware that returns a 304 Not Modified response for responses with
  Last-Modified headers."
  (:require [ring.util.time :refer [parse-date]]
            [ring.util.response :refer [status get-header header]]
            [ring.util.io :refer [close!]]))

(defn- etag-match? [request response]
  (if-let [etag (get-header response "ETag")]
    (= etag (get-header request "if-none-match"))))

(defn- ^java.util.Date date-header [response header]
  (if-let [http-date (get-header response header)]
    (parse-date http-date)))

(defn- not-modified-since? [request response]
  (let [modified-date  (date-header response "Last-Modified")
        modified-since (date-header request "if-modified-since")]
    (and modified-date
         modified-since
         (not (.before modified-since modified-date)))))

(defn- read-request? [request]
  (#{:get :head} (:request-method request)))

(defn- ok-response? [response]
  (= (:status response) 200))

(defn not-modified-response
  "Returns 304 or original response based on response and request.
  See: wrap-not-modified."
  {:added "1.2"}
  [response request]
  (if (and (read-request? request)
           (ok-response? response)
           (or (etag-match? request response)
               (not-modified-since? request response)))
    (do (close! (:body response))
        (-> response
            (assoc :status 304)
            (header "Content-Length" 0)
            (assoc :body nil)))
    response))

(defn wrap-not-modified
  "Middleware that returns a 304 Not Modified from the wrapped handler if the
  handler response has an ETag or Last-Modified header, and the request has a
  If-None-Match or If-Modified-Since header that matches the response."
  {:added "1.2"}
  [handler]
  (fn
    ([request]
     (-> (handler request) (not-modified-response request)))
    ([request respond raise]
     (handler request
              (fn [response] (respond (not-modified-response response request)))
              raise))))

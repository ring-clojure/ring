(ns ring.middleware.not-modified
  "Middleware to return a 304 Not Modified response."
  (:use [ring.util.time :only (parse-date)]
        [ring.util.response :only (status)]
        [ring.util.io :only (close!)]))

(defn- etag-match? [request response]
  (if-let [etag (get-in response [:headers "etag"])]
    (= etag (get-in request [:headers "if-none-match"]))))

(defn- ^java.util.Date date-header [response header]
  (if-let [http-date (get-in response [:headers header])]
    (parse-date http-date)))

(defn- not-modified-since? [request response]
  (let [modified-date  (date-header response "last-modified")
        modified-since (date-header request "if-modified-since")]
    (and modified-date
         modified-since
         (.before modified-since modified-date))))

(defn wrap-not-modified
  "Middleware that returns a 304 Not Modified from the wrapped handler if the
  handler response has an ETag or Last-Modified header, and the request has a
  If-None-Match or If-Modified-Since header that matches the response."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (or (etag-match? request response)
              (not-modified-since? request response))
        (do (close! (:body response))
            (-> response
                (status 304)
                (assoc :body nil)))
        response))))
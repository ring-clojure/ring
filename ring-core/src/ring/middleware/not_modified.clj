(ns ring.middleware.not-modified
  "Middleware to return a 304 Not Modified response."
  (:use [ring.util.time :only (parse-date)]
        [ring.util.response :only (status get-header header)]
        [ring.util.io :only (close!)]))

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

(defn not-modified-response
  "Returns 304 or original response based on response and request."
  [response original-req]
  (if (or (etag-match? original-req response)
          (not-modified-since? original-req response))
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
  [handler]
  (fn [request]
    (-> request
        (handler)
        (not-modified-response request))))

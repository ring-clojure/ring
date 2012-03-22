(ns ring.middleware.head
  "Middleware to simplify replying to HEAD requests.")

(defn wrap-head
  "Middleware that turns any HEAD request into a GET, and then sets the response
  body to nil."
  [handler]
  (fn [request]
    (if (= :head (:request-method request))
      (-> request
          (assoc :request-method :get)
          (handler)
          (assoc :body nil))
      (handler request))))
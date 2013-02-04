(ns ring.middleware.head
  "Middleware to simplify replying to HEAD requests.")

(defn head-request
  "Turns a HEAD request into a GET."
  [request]
  (if (= :head (:request-method request))
    (assoc request :request-method :get)
    request))

(defn head-response
  "Returns a nil body if original request was a HEAD."
  [response request]
  (if (= :head (:request-method request))
    (assoc response :body nil)
    response))

(defn wrap-head
  "Middleware that turns any HEAD request into a GET, and then sets the response
  body to nil."
  [handler]
  (fn [request]
    (-> request
        head-request
        handler
        (head-response request))))
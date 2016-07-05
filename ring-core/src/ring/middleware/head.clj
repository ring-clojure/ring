(ns ring.middleware.head
  "Middleware to simplify replying to HEAD requests.

  A response to a HEAD request should be identical to a GET request, with the
  exception that a response to a HEAD request should have an empty body.")

(defn head-request
  "Turns a HEAD request into a GET."
  {:added "1.2"}
  [request]
  (if (= :head (:request-method request))
    (assoc request :request-method :get)
    request))

(defn head-response
  "Returns a nil body if original request was a HEAD."
  {:added "1.2"}
  [response request]
  (if (and response (= :head (:request-method request)))
    (assoc response :body nil)
    response))

(defn wrap-head
  "Middleware that turns any HEAD request into a GET, and then sets the response
  body to nil."
  {:added "1.1"}
  [handler]
  (fn
    ([request]
     (-> request
         head-request
         handler
         (head-response request)))
    ([request respond raise]
     (handler (head-request request)
              (fn [response] (respond (head-response response request)))
              raise))))

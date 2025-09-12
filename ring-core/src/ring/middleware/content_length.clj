(ns ring.middleware.content-length
  (:require [ring.util.response :as resp]))

(defprotocol SizableResponseBody
  (body-size-in-bytes [body response]
    "Return the number of bytes that an object will require when it is
    serialized as a response body. This number will be placed in the
    Content-Length header of the response by the wrap-content-length
    middleware. If the number of bytes cannot be ascertained, nil is
    returned."))

;; Extending primitive arrays prior to Clojure 1.12 requires using the low-level
;; extend function.
(extend (Class/forName "[B")
  SizableResponseBody
  {:body-size-in-bytes
   (fn [bs _] (alength bs))})

(extend-protocol SizableResponseBody
  String
  (body-size-in-bytes [s response]
    (when-let [charset (resp/get-charset response)]
      (alength (.getBytes s charset))))
  java.io.File
  (body-size-in-bytes [f _]
    (.length f))
  Object
  (body-size-in-bytes [_ _] nil)
  nil
  (body-size-in-bytes [_ _] 0))

(defn content-length-response
  "Adds a Content-Length header to the response. See: wrap-content-length."
  {:added "1.15"}
  [response]
  (when response
    (if (resp/get-header response "content-length")
      response
      (if-let [size (body-size-in-bytes (:body response) response)]
        (-> response (resp/header "Content-Length" (str size)))
        response))))

(defn wrap-content-length
  "Middleware that adds a Content-Length header to the response, if the
  response body satisfies the ContentLength protocol."
  {:added "1.15"}
  [handler]
  (fn
    ([request]
     (content-length-response (handler request)))
    ([request respond raise]
     (handler request
              (comp respond content-length-response)
              raise))))

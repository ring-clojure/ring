(ns ring.middleware.test.absolute-uri
  (:use clojure.test
        ring.middleware.absolute-uri))

(defn- test-handler [req]
  {:status 200
   :headers {}
   :body (:absolute-uri req)})

(def base-req
  {:server-port 80
   :server-name "example.com"
   :uri "/"
   :query-string nil
   :scheme :http})

(deftest absolute-uri-test
  (let [handler (wrap-absolute-uri test-handler)]
    (are [req body] (= (:body (handler req)) body)
      base-req "http://example.com/"
      (assoc base-req :server-port 8080) "http://example.com:8080/"
      (assoc base-req :uri "/abc/def") "http://example.com/abc/def"
      (assoc base-req :query-string "a=b&c=d") "http://example.com/?a=b&c=d"
      (assoc base-req :scheme :https) "https://example.com/")))

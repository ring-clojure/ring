(ns ring.middleware.test.head
  (:use clojure.test
        ring.middleware.head))

(defn- handler [req]
  {:status 200
   :headers {"X-method" (name (:request-method req))}
   :body "Foobar"})

(deftest test-wrap-head
  (let [resp ((wrap-head handler) {:request-method :head})]
    (is (nil? (:body resp)))
    (is (= "get" (get-in resp [:headers "X-method"]))))
  (let [resp ((wrap-head handler) {:request-method :post})]
    (is (= (:body resp) "Foobar"))
    (is (= "post" (get-in resp [:headers "X-method"])))))

(deftest head-request-test
  (is (fn? head-request)))

(deftest head-response-test
  (is (fn? head-response))
  (is (nil? (head-response nil {:request-method :head}))))

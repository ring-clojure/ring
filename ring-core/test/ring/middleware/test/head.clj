(ns ring.middleware.test.head
  (:require [clojure.test :refer :all]
            [ring.middleware.head :refer :all]))

(defn- handler
  ([req]
   {:status 200
    :headers {"X-method" (name (:request-method req))}
    :body "Foobar"})
  ([req cont]
   (cont
    {:status 200
     :headers {"X-method" (name (:request-method req))}
     :body "Foobar"})))

(deftest test-wrap-head
  (let [resp ((wrap-head handler) {:request-method :head})]
    (is (nil? (:body resp)))
    (is (= "get" (get-in resp [:headers "X-method"]))))
  (let [resp ((wrap-head handler) {:request-method :post})]
    (is (= (:body resp) "Foobar"))
    (is (= "post" (get-in resp [:headers "X-method"])))))

(deftest wrap-head-cps-test
  (testing "HEAD request"
    (let [response (promise)]
      ((wrap-head handler) {:request-method :head} response)
      (is (nil? (:body @response)))
      (is (= "get" (get-in @response [:headers "X-method"])))))

  (testing "POST request"
    (let [response (promise)]
      ((wrap-head handler) {:request-method :post} response)
      (is (= "Foobar" (:body @response)))
      (is (= "post" (get-in @response [:headers "X-method"]))))))

(deftest head-request-test
  (is (fn? head-request)))

(deftest head-response-test
  (is (fn? head-response))
  (is (nil? (head-response nil {:request-method :head}))))

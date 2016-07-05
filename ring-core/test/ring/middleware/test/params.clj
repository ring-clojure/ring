(ns ring.middleware.test.params
  (:require [clojure.test :refer :all]
            [ring.middleware.params :refer :all]
            [ring.util.io :refer [string-input-stream]]))

(def wrapped-echo (wrap-params identity))

(deftest wrap-params-query-params-only
  (let [req  {:query-string "foo=bar&biz=bat%25"}
        resp (wrapped-echo req)]
    (is (= {"foo" "bar" "biz" "bat%"} (:query-params resp)))
    (is (empty? (:form-params resp)))
    (is (= {"foo" "bar" "biz" "bat%"} (:params resp)))))

(deftest wrap-params-query-and-form-params
  (let [req  {:query-string "foo=bar"
              :headers      {"content-type" "application/x-www-form-urlencoded"}
              :body         (string-input-stream "biz=bat%25")}
        resp (wrapped-echo req)]
    (is (= {"foo" "bar"}  (:query-params resp)))
    (is (= {"biz" "bat%"} (:form-params resp)))
    (is (= {"foo" "bar" "biz" "bat%"} (:params resp)))))

(deftest wrap-params-not-form-encoded
  (let [req  {:headers {"content-type" "application/json"}
              :body    (string-input-stream "{foo: \"bar\"}")}
        resp (wrapped-echo req)]
    (is (empty? (:form-params resp)))
    (is (empty? (:params resp)))))

(deftest wrap-params-always-assocs-maps
  (let [req  {:query-string ""
              :headers      {"content-type" "application/x-www-form-urlencoded"}
              :body         (string-input-stream "")}
        resp (wrapped-echo req)]
    (is (= {} (:query-params resp)))
    (is (= {} (:form-params resp)))
    (is (= {} (:params resp)))))

(deftest wrap-params-encoding
  (let [req  {:headers {"content-type" "application/x-www-form-urlencoded;charset=UTF-16"}
              :body (string-input-stream "hello=world" "UTF-16")}
        resp (wrapped-echo req)]
    (is (= (:params resp) {"hello" "world"}))
    (is (= (:form-params resp) {"hello" "world"}))))

(deftest wrap-params-cps-test
  (let [handler   (wrap-params (fn [req respond _] (respond req)))
        request   {:query-string "foo=bar"
                   :headers      {"content-type" "application/x-www-form-urlencoded"}
                   :body         (string-input-stream "biz=bat%25")}
        response  (promise)
        exception (promise)]
    (handler request response exception)
    (is (= {"foo" "bar"}  (:query-params @response)))
    (is (= {"biz" "bat%"} (:form-params @response)))
    (is (= {"foo" "bar" "biz" "bat%"} (:params @response)))
    (is (not (realized? exception)))))

(deftest params-request-test
  (is (fn? params-request)))

(deftest assoc-form-params-test
  (is (fn? assoc-form-params)))

(ns ring.middleware.params-test
  (:use clojure.test
        ring.middleware.params)
  (:require [ring.util.test :as test :as tu]))

(def wrapped-echo (wrap-params identity))

(deftest wrap-params-query-params-only
  (let [req  {:query-string "foo=bar&biz=bat%25"}
        resp (wrapped-echo req)]
    (is (= {"foo" "bar" "biz" "bat%"} (:query-params resp)))
    (is (empty? (:form-params resp)))
    (is (= {"foo" "bar" "biz" "bat%"} (:params resp)))))

(deftest wrap-params-query-and-form-params
  (let [req  {:query-string "foo=bar"
              :content-type "application/x-www-form-urlencoded"
              :body         (tu/string-input-stream "biz=bat%25")}
        resp (wrapped-echo req)]
    (is (= {"foo" "bar"}  (:query-params resp)))
    (is (= {"biz" "bat%"} (:form-params resp)))
    (is (= {"foo" "bar" "biz" "bat%"} (:params resp)))))

(deftest wrap-params-not-form-encoded
  (let [req  {:content-type "application/json"
              :body         (tu/string-input-stream "{foo: \"bar\"}")}
        resp (wrapped-echo req)]
    (is (empty? (:form-params resp)))
    (is (empty? (:params resp)))))

(deftest wrap-params-always-assocs-maps
  (let [req  {:query-string ""
              :content-type "application/x-www-form-urlencoded"
              :body         (tu/string-input-stream "")}
        resp (wrapped-echo req)]
    (is (= {} (:query-params resp)))
    (is (= {} (:form-params resp)))
    (is (= {} (:params resp)))))

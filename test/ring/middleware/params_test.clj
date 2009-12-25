(ns ring.middleware.params-test
  (:use (clj-unit core)
        (ring.middleware params))
  (:import (java.io ByteArrayInputStream)))

(defn- str-input-stream [#^String s]
  (ByteArrayInputStream. (.getBytes s)))

(def wrapped-echo (wrap-params identity))

(deftest "wrap-params: query-params only"
  (let [req  {:query-string "foo=bar&biz=bat%25"}
        resp (wrapped-echo req)]
    (assert= {"foo" "bar" "biz" "bat%"} (:query-params resp))
    (assert-nil (:form-params resp))
    (assert= {"foo" "bar" "biz" "bat%"} (:params resp))))

(deftest "wrap-params: query-params and form-params"
  (let [req  {:query-string "foo=bar"
              :content-type "application/x-www-form-urlencoded"
              :body         (str-input-stream "biz=bat%25")}
        resp (wrapped-echo req)]
    (assert= {"foo" "bar"}  (:query-params resp))
    (assert= {"biz" "bat%"} (:form-params resp))
    (assert= {"foo" "bar" "biz" "bat%"} (:params resp))))

(deftest "wrap-params: not form encoded"
  (let [req  {:content-type "application/json"
              :body         (str-input-stream "{foo: \"bar\"}")}
        resp (wrapped-echo req)]
    (assert-nil (:form-params resp))
    (assert-nil (:params resp))))

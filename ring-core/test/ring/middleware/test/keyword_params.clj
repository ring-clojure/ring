(ns ring.middleware.test.keyword-params
  (:require [clojure.test :refer :all]
            [ring.middleware.keyword-params :refer :all]))

(def wrapped-echo (wrap-keyword-params identity))

(deftest test-wrap-keyword-params
  (are [in out] (= out (:params (wrapped-echo {:params in})))
    {"foo" "bar" "biz" "bat"}
    {:foo  "bar" :biz  "bat"}
    {"foo" "bar" "biz" [{"bat" "one"} {"bat" "two"}]}
    {:foo  "bar" :biz  [{:bat "one"}  {:bat  "two"}]}
    {"foo" 1}
    {:foo  1}
    {"foo" 1 "1bar" 2 "baz*" 3 "quz-buz" 4 "biz.bang" 5}
    {:foo 1 "1bar" 2 :baz* 3 :quz-buz 4 "biz.bang" 5}
    {:foo "bar"}
    {:foo "bar"}
    {"foo" {:bar "baz"}}
    {:foo {:bar "baz"}}))

(deftest wrap-keyword-params-cps-test
  (let [handler   (wrap-keyword-params (fn [req cont _] (cont (:params req))))
        response  (promise)
        exception (promise)]
    (handler {:params {"foo" "bar" :baz "quz"}} response exception)
    (is (= {:foo "bar" :baz "quz"} @response))
    (is (not (realized? exception)))))

(deftest keyword-params-request-test
  (is (fn? keyword-params-request)))

(ns ring.middleware.test.keyword-params
  (:use clojure.test
        ring.middleware.keyword-params))

(def wrapped-echo (wrap-keyword-params identity))

(deftest test-wrap-keyword-params
  (are [in out] (= out (:params (wrapped-echo {:params in})))
    {"foo" "bar" "biz" "bat"}
    {:foo  "bar" :biz  "bat"}
    {"foo" "bar" "biz" [{"bat" "one"} {"bat" "two"}]}
    {:foo  "bar" :biz  [{:bat "one"}  {:bat  "two"}]}
    {"foo" 1}
    {:foo  1}))

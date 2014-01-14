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
    {:foo  1}
    {"foo" 1 "1bar" 2 "baz*" 3 "quz-buz" 4 "biz.bang" 5}
    {:foo 1 "1bar" 2 :baz* 3 :quz-buz 4 "biz.bang" 5}
    {:foo "bar"}
    {:foo "bar"}
    {"foo" {:bar "baz"}}
    {:foo {:bar "baz"}}
    {"ns/foo" "bar" "ns/wtf/foo" "baz"}
    {:ns/foo "bar" "ns/wtf/foo" "baz"}
    {"dotted.ns/foo" "bar"}
    {:dotted.ns/foo "bar"}))

(deftest keyword-params-request-test
  (is (fn? keyword-params-request)))

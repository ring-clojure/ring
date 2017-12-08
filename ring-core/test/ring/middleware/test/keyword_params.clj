(ns ring.middleware.test.keyword-params
  (:require [clojure.test :refer :all]
            [ring.middleware.keyword-params :refer :all]))

(def wrapped-echo         (wrap-keyword-params identity))
(def wrapped-echo-with-ns (wrap-keyword-params identity {:parse-namespaces? true}))

(deftest test-wrap-keyword-params
  (testing "plain keywords"
    (are [in out] (= out (:params (wrapped-echo {:params in})))
      {"foo" "bar" "biz" "bat"}
      {:foo "bar" :biz "bat"}
      {"foo" "bar" "biz" [{"bat" "one"} {"bat" "two"}]}
      {:foo "bar" :biz [{:bat "one"}  {:bat "two"}]}
      {"foo" 1}
      {:foo 1}
      {"foo" 1 "1bar" 2 "baz*" 3 "quz-buz" 4 "biz.bang" 5}
      {:foo 1 "1bar" 2 :baz* 3 :quz-buz 4 "biz.bang" 5}
      {:foo "bar"}
      {:foo "bar"}
      {"foo" {:bar "baz"}}
      {:foo {:bar "baz"}}
      {"fòö" "bar" "bíz" "bat"}
      {:fòö  "bar" :bíz  "bat"}
      {"ns/foo" "bar"}
      {"ns/foo" "bar"}))

  (testing "namespaced keywords"
    (are [in out] (= out (:params (wrapped-echo-with-ns {:params in})))
      {"ns/foo" "bar" "ns/wtf/foo" "baz" "foo/1" "fizz" "foo/bar1" "buzz"}
      {:ns/foo "bar" "ns/wtf/foo" "baz" "foo/1" "fizz" :foo/bar1 "buzz"}
      {"dotted.ns/foo" "bar" "dotted.ns/1" "baz" "dotted.ns/bar1" "buzz"}
      {:dotted.ns/foo "bar" "dotted.ns/1" "baz" :dotted.ns/bar1 "buzz"}
      {"fò/bö" "bar" "a.b.í/z" "bat"}
      {:fò/bö  "bar" :a.b.í/z  "bat"}
      {"foo" "bar"}
      {:foo "bar"})))

(deftest wrap-keyword-params-cps-test
  (let [handler   (wrap-keyword-params (fn [req respond _] (respond (:params req))))
        response  (promise)
        exception (promise)]
    (handler {:params {"foo" "bar" :baz "quz"}} response exception)
    (is (= {:foo "bar" :baz "quz"} @response))
    (is (not (realized? exception)))))

(deftest keyword-params-request-test
  (is (fn? keyword-params-request)))

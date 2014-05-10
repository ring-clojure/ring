(ns ring.middleware.test.keyword-params
  (:use clojure.test
        ring.middleware.keyword-params))

(def wrapped-echo (wrap-keyword-params identity))
(def wrapped-echo-with-ns (wrap-keyword-params identity {:allow-namespaces? true}))

(deftest test-wrap-keyword-params
  (testing "default behavior"
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
         {"ns/foo" "bar"}
         {"ns/foo" "bar"}))
  (testing "allow-namespaces? true"
    (are [in out] (= out (:params (wrapped-echo-with-ns {:params in})))
         {"ns/foo" "bar" "ns/wtf/foo" "baz" "foo/1" "fizz" "foo/bar1" "buzz"}
         {:ns/foo "bar" "ns/wtf/foo" "baz" "foo/1" "fizz" :foo/bar1 "buzz"}
         {"dotted.ns/foo" "bar" "dotted.ns/1" "baz" "dotted.ns/bar1" "buzz"}
         {:dotted.ns/foo "bar" "dotted.ns/1" "baz" :dotted.ns/bar1 "buzz"})))

(deftest keyword-params-request-test
  (is (fn? keyword-params-request)))

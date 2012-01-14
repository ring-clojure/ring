(ns ring.util.test.request
  (:use clojure.test
        ring.util.request))

(deftest test-header-seq
  (are [k v hs] (= (header-seq {:headers {k v}} k) hs)
    "accept" "text/html, text/xml" ["text/html" "text/xml"]
    "content-type" "text/html" ["text/html"]
    "x-foo" nil nil))

(deftest test-accept
  (are [a b] (= (accept {:headers {"accept" a}}) b)
    "text/html, text/xml"
    [{:value "text/html" :q 1.0} {:value "text/xml" :q 1.0}]
    "text/html"
    [{:value "text/html" :q 1.0}]
    "text/xml;q=0.5, application/json"
    [{:value "text/xml" :q 0.5} {:value "application/json" :q 1.0}]
    nil
    nil))

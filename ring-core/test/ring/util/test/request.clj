(ns ring.util.test.request
  (:use clojure.test
        ring.util.request))

(deftest test-header-seq
  (are [r k hs] (= (header-seq r k) hs)
    {:headers {"accept" "text/html, text/xml"}}
    "accept"
    ["text/html" "text/xml"]

    {:headers {"content-type" "text/html"}}
    "content-type"
    ["text/html"]

    {:headers {}}
    "x-foo"
    nil))

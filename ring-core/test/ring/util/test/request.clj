(ns ring.util.test.request
  (:use clojure.test
        ring.util.request))

(deftest test-header-seq
  (are [k v hs] (= (header-seq {:headers {k v}} k) hs)
    "accept" "text/html, text/xml" ["text/html" "text/xml"]
    "content-type" "text/html" ["text/html"]
    "x-foo" nil nil))

(ns ring.util.test.request
  (:use clojure.test
        ring.util.request))

(deftest test-request-url
  (is (= (request-url {:scheme :http
                       :uri "/foo/bar"
                       :headers {"host" "example.com"}
                       :query-string "x=y"})
         "http://example.com/foo/bar?x=y"))
  (is (= (request-url {:scheme :http
                       :uri "/"
                       :headers {"host" "localhost:8080"}})
         "http://localhost:8080/"))
  (is (= (request-url {:scheme :https
                       :uri "/index.html"
                       :headers {"host" "www.example.com"}})
         "https://www.example.com/index.html")))
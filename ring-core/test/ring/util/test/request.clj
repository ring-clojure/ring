(ns ring.util.test.request
  (:import java.io.File)
  (:use clojure.test
        ring.util.request
        ring.util.io))

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

(deftest test-body-string
  (testing "nil body"
    (is (= (body-string {:body nil}) nil)))
  (testing "string body"
    (is (= (body-string {:body "foo"}) "foo")))
  (testing "file body"
    (let [f (File/createTempFile "ring-test" "")]
      (spit f "bar")
      (is (= (body-string {:body f}) "bar"))
      (.delete f)))
  (testing "input-stream body"
    (is (= (body-string {:body (string-input-stream "baz")}) "baz"))))

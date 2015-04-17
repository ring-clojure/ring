(ns ring.util.test.request
  (:require [clojure.test :refer :all]
            [ring.util.request :refer :all]
            [ring.util.io :refer [string-input-stream]])
  (:import [java.io File]))

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

(deftest test-content-type
  (testing "no content-type"
    (is (= (content-type {:headers {}}) nil)))
  (testing "content type with no charset"
    (is (= (content-type {:headers {"content-type" "text/plain"}}) "text/plain")))
  (testing "content type with charset"
    (is (= (content-type {:headers {"content-type" "text/plain; charset=UTF-8"}})
           "text/plain"))))

(deftest test-content-length
  (testing "no content-length header"
    (is (= (content-length {:headers {}}) nil)))
  (testing "a content-length header"
    (is (= (content-length {:headers {"content-length" "1337"}}) 1337))))

(deftest test-character-encoding
  (testing "no content-type"
    (is (= (character-encoding {:headers {}}) nil)))
  (testing "content-type with no charset"
    (is (= (character-encoding {:headers {"content-type" "text/plain"}}) nil)))
  (testing "content-type with charset"
    (is (= (character-encoding {:headers {"content-type" "text/plain; charset=UTF-8"}})
           "UTF-8"))
    (is (= (character-encoding {:headers {"content-type" "text/plain;charset=UTF-8"}})
           "UTF-8"))))

(deftest test-urlencoded-form?
  (testing "urlencoded form"
    (is (urlencoded-form? {:headers {"content-type" "application/x-www-form-urlencoded"}}))
    (is (urlencoded-form?
         {:headers {"content-type" "application/x-www-form-urlencoded; charset=UTF-8"}})))
  (testing "other content type"
    (is (not (urlencoded-form? {:headers {"content-type" "application/json"}}))))
  (testing "no content type"
    (is (not (urlencoded-form? {:headers {}})))))

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

(deftest test-in-context?
  (is (in-context? {:uri "/foo/bar"} "/foo"))
  (is (not (in-context? {:uri "/foo/bar"} "/bar"))))

(deftest test-set-context
  (is (= (set-context {:uri "/foo/bar"} "/foo")
         {:uri "/foo/bar"
          :context "/foo"
          :path-info "/bar"}))
  (is (thrown? AssertionError (set-context {:uri "/foo/bar"} "/bar"))))

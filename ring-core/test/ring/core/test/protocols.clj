(ns ring.core.test.protocols
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [ring.core.protocols :refer :all]))

(deftest test-write-body-defaults
  (testing "strings"
    (let [output   (java.io.ByteArrayOutputStream.)
          response {:body "Hello World"}]
      (write-body-to-stream (:body response) response output)
      (is (= "Hello World" (.toString output)))))

  (testing "strings with encoding"
    (let [output   (java.io.ByteArrayOutputStream.)
          response {:headers {"Content-Type" "text/plain; charset=UTF-16"}
                    :body    "Hello World"}]
      (write-body-to-stream (:body response) response output)
      (is (= "Hello World" (.toString output "UTF-16")))))

  (testing "seqs"
    (let [output   (java.io.ByteArrayOutputStream.)
          response {:body (list "Hello" " " "World")}]
      (write-body-to-stream (:body response) response output)
      (is (= "Hello World" (.toString output)))))

  (testing "seqs with encoding"
    (let [output   (java.io.ByteArrayOutputStream.)
          response {:headers {"Content-Type" "text/plain; charset=UTF-16"}
                    :body    (list "Hello" " " "World")}]
      (write-body-to-stream (:body response) response output)
      (is (= "Hello World" (.toString output "UTF-16")))))

  (testing "input streams"
    (let [output   (java.io.ByteArrayOutputStream.)
          response {:body (io/input-stream (io/resource "ring/assets/hello world.txt"))}]
      (write-body-to-stream (:body response) response output)
      (is (= "Hello World\n" (.toString output)))))

  (testing "files"
    (let [output   (java.io.ByteArrayOutputStream.)
          response {:body (io/file "test/ring/assets/hello world.txt")}]
      (write-body-to-stream (:body response) response output)
      (is (= "Hello World\n" (.toString output)))))

  (testing "nil"
    (let [output   (java.io.ByteArrayOutputStream.)
          response {:body nil}]
      (write-body-to-stream (:body response) response output)
      (is (= "" (.toString output))))))

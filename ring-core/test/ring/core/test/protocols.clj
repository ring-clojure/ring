(ns ring.core.test.protocols
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [ring.core.protocols :refer :all]))

(deftest test-write-body-defaults
  (testing "strings"
    (let [output (java.io.ByteArrayOutputStream.)]
      (write-body-to-stream "Hello World" output)
      (is (= "Hello World" (.toString output)))))

  (testing "seqs"
    (let [output (java.io.ByteArrayOutputStream.)]
      (write-body-to-stream (list "Hello" " " "World") output)
      (is (= "Hello World" (.toString output)))))

  (testing "input streams"
    (let [output (java.io.ByteArrayOutputStream.)
          input  (io/input-stream (io/resource "ring/assets/hello world.txt"))]
      (write-body-to-stream input output)
      (is (= "Hello World\n" (.toString output)))))

  (testing "files"
    (let [output (java.io.ByteArrayOutputStream.)]
      (write-body-to-stream (io/file "test/ring/assets/hello world.txt") output)
      (is (= "Hello World\n" (.toString output))))))

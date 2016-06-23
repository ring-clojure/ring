(ns ring.core.test.protocols
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [ring.core.protocols :refer :all]))

(deftest test-write-body-defaults
  (testing "strings"
    (let [output (java.io.ByteArrayOutputStream.)]
      (write-body "Hello World" output)
      (is (= "Hello World" (.toString output)))))

  (testing "seqs"
    (let [output (java.io.ByteArrayOutputStream.)]
      (write-body (list "Hello" " " "World") output)
      (is (= "Hello World" (.toString output)))))

  (testing "input streams"
    (let [output (java.io.ByteArrayOutputStream.)]
      (write-body (io/input-stream (io/resource "ring/assets/hello world.txt")) output)
      (is (= "Hello World\n" (.toString output)))))

  (testing "files"
    (let [output (java.io.ByteArrayOutputStream.)]
      (write-body (io/file "test/ring/assets/hello world.txt") output)
      (is (= "Hello World\n" (.toString output))))))

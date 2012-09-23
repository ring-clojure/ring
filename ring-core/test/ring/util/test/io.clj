(ns ring.util.test.io
  (:use clojure.test
        ring.util.io)
  (:require [clojure.java.io :as io])
  (:import java.io.IOException))

(deftest test-piped-input-stream
  (let [stream (piped-input-stream #(spit % "Hello World"))]
    (is (= (slurp stream) "Hello World"))))

(deftest test-string-input-stream
  (let [stream (string-input-stream "Hello World")]
    (is (= (slurp stream) "Hello World"))))

(deftest test-close!
  (testing "non-streams"
    (is (nil? (close! "foo"))))
  (testing "streams"
    (let [stream (piped-input-stream #(spit % "Hello World"))]
      (close! stream)
      (is (thrown? IOException (slurp stream)))
      (is (nil? (close! stream))))))
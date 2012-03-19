(ns ring.util.test.io
  (:use clojure.test
        ring.util.io)
  (:require [clojure.java.io :as io]))

(deftest test-piped-input-stream
  (let [stream (piped-input-stream #(spit % "Hello World"))]
    (is (= (slurp stream) "Hello World"))))

(deftest test-string-input-stream
  (let [stream (string-input-stream "Hello World")]
    (is (= (slurp stream) "Hello World"))))
(ns ring.core.test.protocols
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [ring.core.protocols :refer :all])
  (:import [java.io SequenceInputStream IOException InputStream OutputStream]))

(deftest test-write-body-defaults
  (testing "byte-array"
    (let [output   (java.io.ByteArrayOutputStream.)
          response {:body (.getBytes "Hello World")}]
      (write-body-to-stream (:body response) response output)
      (is (= "Hello World" (.toString output)))))

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

(defn output-stream-with-close-flag []
  (let [stream-closed? (atom false)
        output-stream  (proxy [OutputStream] []
                         (write
                           ([])
                           ([^bytes _])
                           ([^bytes _ _ _]))
                         (close [] (reset! stream-closed? true)))]
    [output-stream stream-closed?]))

(defn error-input-stream []
  (proxy [InputStream] []
    (read
      ([] (throw (IOException. "test error")))
      ([^bytes _] (throw (IOException. "test error")))
      ([^bytes _ _ _] (throw (IOException. "test error"))))))

(deftest test-error-when-writing-body
  (testing "input streams with error"
    (let [[output closed?] (output-stream-with-close-flag)
          resource         (io/resource "ring/assets/hello world.txt")
          input-stream     (SequenceInputStream.
                            (io/input-stream resource)
                            (error-input-stream))
          response         {:body input-stream}]
      (is (thrown? IOException
                   (write-body-to-stream (:body response) response output)))
      (is (= false @closed?))))

  (testing "seqs with error"
    (let [[output closed?] (output-stream-with-close-flag)
          response         {:body (lazy-cat
                                   ["a"]
                                   (throw (IOException. "test error")))}]
      (is (thrown? IOException
                   (write-body-to-stream (:body response) response output)))
      (is (= false @closed?)))))

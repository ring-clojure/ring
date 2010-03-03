(ns ring.util.codec-test
  (:use clojure.test
        ring.util.codec)
  (:import java.util.Arrays))

(deftest test-url-encode
  (is (= "foo%2Fbar" (url-encode "foo/bar")))
  (is (= "foo%FE%FF%00%2Fbar") (url-encode "foo/bar" "UTF-16")))

(deftest test-url-decode
  (is (= "foo/bar" (url-decode "foo%2Fbar")))
  (is (= "foo/bar" (url-decode "foo%FE%FF%00%2Fbar" "UTF-16"))))

(deftest test-base64-encoding
  (let [str-bytes (.getBytes "foo?/+" "UTF-8")]
    (is (Arrays/equals str-bytes (base64-decode (base64-encode str-bytes))))))

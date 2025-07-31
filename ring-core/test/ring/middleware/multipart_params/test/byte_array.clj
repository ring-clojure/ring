(ns ring.middleware.multipart-params.test.byte-array
  (:require [clojure.test :refer :all]
            [ring.middleware.multipart-params.byte-array :refer :all]
            [ring.util.io :refer [string-input-stream]]))

(deftest test-byte-array-store
  (let [store  (byte-array-store)
        result (store
                {:filename "foo.txt"
                 :content-type "text/plain"
                 :stream (string-input-stream "foo")})]
    (is (= (:filename result) "foo.txt"))
    (is (= (:content-type result) "text/plain"))
    (is (= (String. (:bytes result)) "foo"))))

(deftest test-byte-array-store-max-size-success
  (let [store  (byte-array-store {:max-size 100})
        result (store
                {:filename "foo.txt"
                 :content-type "text/plain"
                 :stream (string-input-stream "foo")})]
    (is (= (:filename result) "foo.txt"))
    (is (= (:content-type result) "text/plain"))
    (is (= (String. (:bytes result)) "foo"))))

(deftest test-byte-array-store-max-size-failure
  (let [store  (byte-array-store {:max-size 1})
        result (store
                {:filename "foo.txt"
                 :content-type "text/plain"
                 :stream (string-input-stream "foo")})]
    (is (= (:filename result) "foo.txt"))
    (is (= (:content-type result) "text/plain"))
    (is (= (:bytes result)
           :ring.middleware.multipart-params.byte-array/too-large))))

(ns ring.middleware.multipart-params.test.byte-array
  (:use clojure.test
        ring.util.test
        ring.middleware.multipart-params.byte-array))

(deftest test-byte-array-store
  (let [store  (byte-array-store)
        result (store
                {:filename "foo.txt"
                 :content-type "text/plain"
                 :stream (string-input-stream "foo")})]
    (is (= (:filename result) "foo.txt"))
    (is (= (:content-type result) "text/plain"))
    (is (= (String. (:bytes result)) "foo"))))

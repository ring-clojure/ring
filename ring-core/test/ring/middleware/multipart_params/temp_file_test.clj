(ns ring.middleware.multipart-params.temp-file-test
  (:use clojure.test
        ring.util.test
        ring.middleware.multipart-params.temp-file))

(deftest test-temp-file-store
  (let [store  (temp-file-store)
        result (store
                {:filename "foo.txt"
                 :content-type "text/plain"
                 :stream (string-input-stream "foo")})]
    (is (= (:filename result) "foo.txt"))
    (is (= (:content-type result) "text/plain"))
    (is (= (:size result) 3))
    (is (instance? java.io.File (:tempfile result)))
    (is (.exists (:tempfile result)))
    (is (= (slurp (:tempfile result)) "foo"))))

(deftest test-temp-file-expiry
  (let [store  (temp-file-store {:expires-in 2})
        result (store
                {:filename "foo.txt"
                 :content-type "text/plain"
                 :stream (string-input-stream "foo")})]
    (is (.exists (:tempfile result)))
    (Thread/sleep 2500)
    (is (not (.exists (:tempfile result))))))

(ns ring.util.test.compat
  (:use clojure.test
        ring.util.compat))

(deftest test-reducible?
  (is (reducible? (list 1 2 3))))

(ns ring.util.test.async
  (:require [clojure.test :refer [deftest is]]
            [ring.util.async :refer [raising]]))

(deftest test-raising
  (let [error (promise)]
    ((raising error #(/ 1 0)))
    (is (realized? error))
    (is (instance? ArithmeticException (deref error 0 nil)))))

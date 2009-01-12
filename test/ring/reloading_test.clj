(ns ring.reloading-test
  (:use clj-unit.core ring.reloading))

(def app (constantly :response))

(deftest "wrap"
  (let [wrapped (wrap '(ring.reloading) app)]
    (assert= :response (wrapped :request))))

(ns ring.reload-test
  (:use clj-unit.core ring.reload))

(def app (constantly :response))

(deftest "wrap"
  (let [wrapped (wrap '(ring.reload) app)]
    (assert= :response (wrapped :request))))

(ns ring.middleware.test.reload
  (:use clojure.test
        ring.middleware.reload))

(def app
  (wrap-reload (constantly :response) '(ring.middleware.reload)))

(deftest wrap-reload-smoke
  (is (= :response (app :request))))

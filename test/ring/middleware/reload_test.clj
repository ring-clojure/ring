(ns ring.middleware.reload-test
  (:use (clj-unit core)
        (ring.middleware reload)))

(def app
  (wrap-reload (constantly :response) '(ring.middleware.reload)))

(deftest "wrap-reload"
  (assert= :response (app :request)))

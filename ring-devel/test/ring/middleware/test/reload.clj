(ns ring.middleware.test.reload
  (:use clojure.test
        ring.middleware.reload))

(deftest wrap-reload-smoke-test
  (let [handler (wrap-reload identity)
        request {:http-method :get, :uri "/"}]
    (is (= (handler request) request))))

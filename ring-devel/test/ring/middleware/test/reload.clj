(ns ring.middleware.test.reload
  (:require [clojure.test :refer :all]
            [ring.middleware.reload :refer :all]))

(deftest wrap-reload-smoke-test
  (let [handler (wrap-reload identity)
        request {:http-method :get, :uri "/"}]
    (is (= (handler request) request))))

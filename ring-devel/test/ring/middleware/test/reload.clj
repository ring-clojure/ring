(ns ring.middleware.test.reload
  (:require [clojure.test :refer :all]
            [ring.middleware.reload :refer :all]))

(deftest wrap-reload-smoke-test
  (let [handler (wrap-reload identity)
        request {:http-method :get, :uri "/"}]
    (is (= (handler request) request))))

(deftest wrap-reload-cps-test
  (let [handler   (wrap-reload (fn [req respond _] (respond req)))
        request   {:http-method :get, :uri "/"}
        response  (promise)
        exception (promise)]
    (handler request response exception)
    (is (= request @response))
    (is (not (realized? exception)))))

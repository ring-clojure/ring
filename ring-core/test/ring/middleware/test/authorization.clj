(ns ring.middleware.test.authorization
  (:require
    [clojure.test :refer :all]
    [ring.middleware.authorization :refer :all]))

(deftest wrap-authorization-none-test
  (let [handler   (wrap-authorization (fn [req respond _] (respond req)))
        request   {:headers {}}
        response  (promise)
        exception (promise)]
    (handler request response exception)
    (is (nil? (:authorization @response)))
    (is (not (realized? exception)))))

(deftest wrap-authorization-scheme-only-test
  (let [handler   (wrap-authorization (fn [req respond _] (respond req)))
        request   {:headers {"authorization" "Basic"}}
        response  (promise)
        exception (promise)]
    (handler request response exception)
    (is (= {:params  {}
            :scheme  "basic"}
           (:authorization @response)))
    (is (not (realized? exception)))))

(deftest wrap-authorization-token68-test
  (let [handler   (wrap-authorization (fn [req respond _] (respond req)))
        request   {:headers {"authorization" "Basic dGVzdA=="}}
        response  (promise)
        exception (promise)]
    (handler request response exception)
    (is (= {:params  {}
            :scheme  "basic"
            :token68 "dGVzdA=="}
           (:authorization @response)))
    (is (not (realized? exception)))))

(deftest wrap-authorization-auth-params-test
  (let [handler   (wrap-authorization (fn [req respond _] (respond req)))
        request   {:headers {"authorization" "Basic A=\"B\""}}
        response  (promise)
        exception (promise)]
    (handler request response exception)
    (is (= {:params {"a" "B"}
            :scheme "basic"}
           (:authorization @response)))
    (is (not (realized? exception)))))

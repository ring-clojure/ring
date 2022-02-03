(ns ring.middleware.test.stacktrace
  (:require [clojure.test :refer :all]
            [ring.middleware.stacktrace :refer :all]))

(def exception-app (wrap-stacktrace (fn [_] (throw (Exception. "fail")))))
(def assert-app    (wrap-stacktrace (fn [_] (assert (= 1 2)))))


(def html-req  {:headers {"accept" "text/html"}})
(def js-req    {:headers {"accept" "application/javascript"}})
(def plain-req {})

(deftest wrap-stacktrace-smoke
  (binding [*err* (java.io.StringWriter.)]
    (doseq [app [exception-app assert-app]]
      (testing "requests with Accept: text/html"
        (let [{:keys [status headers body]} (app html-req)]
          (is (= 500 status))
          (is (= {"Content-Type" "text/html"} headers))
          (is (.startsWith body "<!DOCTYPE html>"))))
      (testing "requests with Accept: application/javascript"
        (let [{:keys [status headers body]} (app js-req)]
          (is (= 500 status))
          (is (= {"Content-Type" "text/plain"} headers))
          (is (or (.startsWith body "java.lang.Exception")
                  (.startsWith body "java.lang.AssertionError")))))
      (testing "requests without Accept header"
        (let [{:keys [status headers body]} (app plain-req)]
          (is (= 500 status))
          (is (= {"Content-Type" "text/plain"} headers))
          (is (or (.startsWith body "java.lang.Exception")
                  (.startsWith body "java.lang.AssertionError"))))))))

(deftest wrap-stacktrace-cps-test
  (testing "no exception"
    (let [handler   (wrap-stacktrace (fn [_ respond _] (respond :ok)))
          response  (promise)
          exception (promise)]
      (handler {} response exception)
      (is (= :ok @response))
      (is (not (realized? exception)))))

  (testing "thrown exception"
    (let [handler   (wrap-stacktrace (fn [_ _ _] (throw (Exception. "fail"))))
          response  (promise)
          exception (promise)]
      (binding [*err* (java.io.StringWriter.)]
        (handler {} response exception))
      (is (= 500 (:status @response)))
      (is (not (realized? exception)))))

  (testing "raised exception"
    (let [handler   (wrap-stacktrace (fn [_ _ raise] (raise (Exception. "fail"))))
          response  (promise)
          exception (promise)]
      (binding [*err* (java.io.StringWriter.)]
        (handler {} response exception))
      (is (= 500 (:status @response)))
      (is (not (realized? exception))))))

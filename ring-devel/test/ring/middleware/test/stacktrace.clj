(ns ring.middleware.test.stacktrace
  (:require [clojure.test :refer :all]
            [ring.middleware.stacktrace :refer :all]))

(def exception-app (wrap-stacktrace (fn [_] (throw (Exception. "fail")))))
(def assert-app    (wrap-stacktrace (fn [_] (assert (= 1 2)))))


(def html-req {})
(def js-req   {:headers {"accept" "text/javascript"}})

(deftest wrap-stacktrace-smoke
  (binding [*err* (java.io.StringWriter.)]
    (let [{:keys [status headers] :as response} (exception-app html-req)]
      (is (= 500 status))
      (is (= {"Content-Type" "text/html"} headers)))
    (let [{:keys [status headers]} (exception-app js-req)]
      (is (= 500 status))
      (is (= {"Content-Type" "text/javascript"} headers)))
    (let [{:keys [status headers] :as response} (assert-app html-req)]
      (is (= 500 status))
      (is (= {"Content-Type" "text/html"} headers)))
    (let [{:keys [status headers]} (assert-app js-req)]
      (is (= 500 status))
      (is (= {"Content-Type" "text/javascript"} headers)))))

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

(ns ring.middleware.stacktrace-test
  (:use clojure.test
        ring.middleware.stacktrace))

(def app (wrap-stacktrace #(throw (Exception. "fail"))))

(def html-req {})
(def js-req   {:headers {"accept" "text/javascript"}})

(deftest wrap-stacktrace-smoke
  (let [{:keys [status headers] :as response} (app html-req)]
    (is (= 500 status))
    (is (= {"Content-Type" "text/html"} headers)))
  (let [{:keys [status headers]} (app js-req)]
    (is (= 500 status))
    (is (= {"Content-Type" "text/javascript"} headers))))

(ns ring.backtrace-test
  (:use clj-unit.core ring.backtrace))

(def app (wrap #(throw (Exception. "fail"))))

(def html-req {})
(def js-req   {:headers {"accept" "text/javascript"}})

(deftest "wrap"
  (let [{:keys [status headers] :as response} (app html-req)]
    (assert= 500 status)
    (assert= {"Content-Type" "text/html"} headers))
  (let [{:keys [status headers]} (app js-req)]
    (assert= 500 status)
    (assert= {"Content-Type" "text/javascript"} headers)))

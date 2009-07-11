(ns ring.middleware.stacktrace-test
  (:use (clj-unit core)
        (ring.middleware stacktrace)))

(def app (wrap-stacktrace #(throw (Exception. "fail"))))

(def html-req {})
(def js-req   {:headers {"accept" "text/javascript"}})

(deftest "wrap-stacktrace"
  (let [{:keys [status headers] :as response} (app html-req)]
    (assert= 500 status)
    (assert= {"Content-Type" "text/html"} headers))
  (let [{:keys [status headers]} (app js-req)]
    (assert= 500 status)
    (assert= {"Content-Type" "text/javascript"} headers)))

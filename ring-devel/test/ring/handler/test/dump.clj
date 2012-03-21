(ns ring.handler.test.dump
  (:use clojure.test
        [ring.util.io :only (string-input-stream)]
        ring.handler.dump))

(def post-req
  {:uri            "/foo/bar"
   :request-method :post
   :body           (string-input-stream "post body")})

(def get-req
  {:uri            "/foo/bar"
   :request-method :get})

(deftest test-handle-dump
  (binding [*out* (java.io.StringWriter.)]
    (let [{:keys [status]} (handle-dump post-req)]
      (is (= 200 status)))
    (let [{:keys [status]} (handle-dump get-req)]
      (is (= 200 status)))))

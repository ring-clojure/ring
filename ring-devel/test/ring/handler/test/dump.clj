(ns ring.handler.test.dump
  (:require [clojure.test :refer :all]
            [ring.handler.dump :refer :all]
            [ring.util.io :refer [string-input-stream]]))

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

(deftest handle-dump-cps-test
  (binding [*out* (java.io.StringWriter.)]
    (let [response  (promise)
          exception (promise)]
      (handle-dump post-req response exception)
      (is (= 200 (:status @response)))
      (is (not (realized? exception))))))

(ns ring.handler.dump-test
  (:use (clojure test)
        (ring.handler dump))
  (:import (java.io ByteArrayInputStream)))

(def post-req
  {:uri            "/foo/bar"
   :request-method :post
   :body           (ByteArrayInputStream. (.getBytes "post body"))})

(def get-req
  {:uri            "/foo/bar"
   :request-method :get})

(deftest handler-dump
  (let [{:keys [status]} (handle-dump post-req)]
    (is (= 200 status)))
  (let [{:keys [status]} (handle-dump get-req)]
    (is (= 200 status))))

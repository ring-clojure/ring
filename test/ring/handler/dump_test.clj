(ns ring.handler.dump-test
  (:use (clj-unit core)
        (ring.handler dump))
  (:import (java.io ByteArrayInputStream)))

(def post-req
  {:uri            "/foo/bar"
   :request-method :post
   :body           (ByteArrayInputStream. (.getBytes "post body"))})

(def get-req
  {:uri            "/foo/bar"
   :request-method :get})

(deftest "handle-dump"
  (let [{:keys [status]} (handle-dump post-req)]
    (assert= 200 status))
  (let [{:keys [status]} (handle-dump get-req)]
    (assert= 200 status)))

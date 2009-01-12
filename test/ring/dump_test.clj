(ns ring.dump-test
  (:use clj-unit.core ring.dump)
  (:import java.io.ByteArrayInputStream))

(def post-req
  {:uri            "/foo/bar"
   :request-method :post
   :body           (ByteArrayInputStream. (.getBytes "post body"))})

(def get-req
  {:uri            "/foo/bar"
   :request-method :get})

(deftest "app"
  (let [{:keys [status]} (app post-req)]
    (assert= 200 status))
  (let [{:keys [status]} (app get-req)]
    (assert= 200 status)))

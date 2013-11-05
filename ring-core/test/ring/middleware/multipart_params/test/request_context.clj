(ns ring.middleware.multipart-params.test.request-context
  (:use clojure.test)
  (:require [ring.middleware.multipart-params :as mp]))

(deftest test-default-content-length
  (is (= -1
         (.getContentLength (#'mp/request-context {} nil)))))

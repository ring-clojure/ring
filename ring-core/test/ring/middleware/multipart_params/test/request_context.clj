(ns ring.middleware.multipart-params.test.request-context
  (:require [clojure.test :refer :all]
            [ring.middleware.multipart-params :as mp]))

(deftest test-default-content-length
  (is (= -1
         (.getContentLength (#'mp/request-context {} nil)))))

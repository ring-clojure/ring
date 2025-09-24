(ns ring.middleware.multipart-params.test.request-context
  (:require [clojure.test :refer :all]
            [ring.middleware.multipart-params :as mp])
  (:import [org.apache.commons.fileupload2.core RequestContext]))

(deftest test-default-content-length
  (is (= -1
         (.getContentLength ^RequestContext (#'mp/request-context {} nil)))))

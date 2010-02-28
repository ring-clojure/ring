(ns ring.util.response-test
  (:use clojure.test
        ring.util.response))

(deftest test-redirect
  (is (= {:status 302 :headers {"Location" "http://google.com"} :body ""}
         (redirect "http://google.com"))))

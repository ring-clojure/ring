(ns ring.util.response-test
  (:use clojure.test
        ring.util.response))

(deftest test-redirects-to
  (is (= {:status 302 :headers {"Location" "http://google.com"} :body ""}
         (redirect-to "http://google.com")))
  (is (= {:status 303 :headers {"Location" "http://google.com"} :body ""}
         (redirect-to "http://google.com" {:status 303}))))

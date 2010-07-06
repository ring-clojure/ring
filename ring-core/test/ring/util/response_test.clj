(ns ring.util.response-test
  (:use clojure.test
        ring.util.response
        [clojure.contrib.duck-streams :only (slurp*)]))

(deftest test-redirect
  (is (= {:status 302 :headers {"Location" "http://google.com"} :body ""}
         (redirect "http://google.com"))))

(deftest test-response
  (is (= {:status 200 :headers {} :body "foobar"}
         (response "foobar"))))

(deftest test-status
  (is (= {:status 200 :body ""} (status {:status nil :body ""} 200))))

(deftest test-content-type
  (is (= {:status 200 :headers {"Content-Type" "text/html" "Content-Length" "10"}}
         (content-type {:status 200 :headers {"Content-Length" "10"}} "text/html"))))

(deftest test-resource-response
  (let [resp (resource-response "/ring/util/response_test.clj")]
    (is (= (resp :status) 200))
    (is (= (resp :headers) {}))
    (is (.startsWith (slurp* (resp :body))
                     "(ns ring.util.response-test"))))

(deftest test-missing-resource
  (is (nil? (resource-response "/missing/resource.clj"))))

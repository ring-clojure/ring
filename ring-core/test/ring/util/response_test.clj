(ns ring.util.response-test
  (:use clojure.test
        ring.util.response))

(defmacro with-classloader
  "Temporarily replaces the current context classloader with one that
   includes everything in dir"
  [[dir] & forms]
  `(let [current-thread# (Thread/currentThread)
         original-loader# (.getContextClassLoader current-thread#)
         new-loader# (java.net.URLClassLoader. (into-array [(.toURL ~dir)])
                                               original-loader#)]
     (try
        (.setContextClassLoader current-thread# new-loader#)
        ~@forms
        (finally
          (.setContextClassLoader current-thread# original-loader#)))))

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
    (is (.startsWith (slurp (resp :body))
                     "(ns ring.util.response-test"))))

(deftest test-resource-with-root
  (let [resp (resource-response "response_test.clj" {:root "/ring/util"})]
    (is (.startsWith (slurp (resp :body))
                     "(ns ring.util.response-test"))))

(deftest test-resource-with-child-classloader
  (let [resource (java.io.File/createTempFile "response_test" nil)]
    (org.apache.commons.io.FileUtils/writeStringToFile resource "just testing")
    (with-classloader [(.getParentFile resource)]
      (let [resp (resource-response (.getName resource))]
        (is (= (slurp (resp :body))
              "just testing"))))))

(deftest test-missing-resource
  (is (nil? (resource-response "/missing/resource.clj"))))

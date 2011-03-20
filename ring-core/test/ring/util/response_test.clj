(ns ring.util.response-test
  (:use clojure.test
        ring.util.response)
  (:import [java.io File InputStream]
           org.apache.commons.io.FileUtils))

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
         (content-type {:status 200 :headers {"Content-Length" "10"}}
                       "text/html"))))

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

(deftest test-resource-response
  (testing "response map"
    (let [resp (resource-response "/ring/assets/foo.html")]
      (is (= (resp :status) 200))
      (is (= (resp :headers) {}))
      (is (= (slurp (resp :body)) "foo"))))

  (testing "with root option"
    (let [resp (resource-response "/foo.html" {:root "/ring/assets"})]
      (is (= (slurp (resp :body)) "foo"))))

  (testing "with child class-loader"
    (let [resource (File/createTempFile "response_test" nil)]
      (FileUtils/writeStringToFile resource "just testing")
      (with-classloader [(.getParentFile resource)]
        (let [resp (resource-response (.getName resource))]
          (is (= (slurp (resp :body))
                 "just testing"))))))

  (testing "missing resource"
    (is (nil? (resource-response "/missing/resource.clj"))))

  (testing "response body type"
    (let [body (:body (resource-response "ring/util/response.clj"))]
      (is (instance? File body))
      (is (.startsWith (slurp body) "(ns ring.util.response")))
    (let [body (:body (resource-response "clojure/java/io.clj"))]
      (is (instance? InputStream body))
      (is (.contains (slurp body) "clojure.java.io")))))

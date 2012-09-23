(ns ring.middleware.test.not-modified
  (:use clojure.test
        ring.middleware.not-modified))

(defn- handler-etag [etag]
  (constantly
   {:status 200
    :headers {"etag" etag}
    :body ""}))

(defn- handler-modified [modified]
  (constantly
   {:status 200
    :headers {"last-modified" modified}
    :body ""}))

(defn- etag-request [etag]
  {:headers {"if-none-match" etag}})

(defn- modified-request [modified-date]
  {:headers {"if-modified-since" modified-date}})

(deftest test-wrap-not-modified
  (testing "etags"
    (let [h (wrap-not-modified (handler-etag "\"12345\""))]
      (is (= 304 (:status (h (etag-request "\"12345\"")))))
      (is (= 200 (:status (h (etag-request "\"abcde\"")))))))
  (testing "last-modified"
    (let [h (wrap-not-modified (handler-modified "Sun, 23 Sep 2012 10:52:50 GMT"))]
      (is (= 304 (:status (h (modified-request "Sun, 23 Sep 2012 10:00:00 GMT")))))
      (is (= 200 (:status (h (modified-request "Sun, 23 Sep 2012 11:00:00 GMT")))))))
  (testing "no modified info"
    (let [h (wrap-not-modified (constantly {:status 200 :headers {} :body ""}))]
      (is (= 200 (:status (h (etag-request "\"12345\"")))))
      (is (= 200 (:status (h (modified-request "Sun, 23 Sep 2012 10:00:00 GMT"))))))))
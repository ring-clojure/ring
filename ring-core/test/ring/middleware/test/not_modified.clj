(ns ring.middleware.test.not-modified
  (:require [clojure.test :refer :all]
            [ring.middleware.not-modified :refer :all]))

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
  {:request-method :get, :headers {"if-none-match" etag}})

(defn- modified-request [modified-date]
  {:request-method :get, :headers {"if-modified-since" modified-date}})

(deftest test-wrap-not-modified
  (with-redefs [ring.middleware.not-modified/not-modified-response #(vector %1 %2)]
    (let [req (modified-request "Sun, 23 Sep 2012 11:00:00 GMT")
          handler (handler-modified "Jan, 23 Sep 2012 11:00:00 GMT")]
      (is (= [(handler req) req] ; Not modified since is called with expected args
             ((wrap-not-modified handler) req))))))

(deftest wrap-not-modified-cps-test
  (testing "etag match"
    (let [etag      "known-etag"
          handler   (wrap-not-modified
                     (fn [req respond _]
                       (respond {:status 200, :headers {"etag" etag}, :body ""})))
          request   {:request-method :get, :headers {"if-none-match" etag}}
          response  (promise)
          exception (promise)]
      (handler request response exception)
      (is (= 304 (:status @response)))
      (is (not (realized? exception))))))

(deftest test-not-modified-response  
  (testing "etag match"
    (let [known-etag "known-etag"
          request {:request-method :get, :headers {"if-none-match" known-etag}}
          handler-resp #(hash-map :status 200 :headers {"etag" %} :body "")]
      (is (= 304 (:status (not-modified-response (handler-resp known-etag) request))))
      (is (= 200 (:status (not-modified-response (handler-resp "unknown-etag") request))))))
  
  (testing "not modified"
    (let [req #(hash-map :request-method :get, :headers {"if-modified-since" %})
          last-modified "Sun, 23 Sep 2012 11:00:00 GMT"
          h-resp {:status 200 :headers {"Last-Modified" last-modified} :body ""}]
      (is (= 304 (:status (not-modified-response h-resp (req last-modified)))))
      (is (= 304 (:status (not-modified-response h-resp (req "Sun, 23 Sep 2012 11:52:50 GMT")))))
      (is (= 200 (:status (not-modified-response h-resp (req "Sun, 23 Sep 2012 10:00:50 GMT")))))))

  (testing "not modified body and content-length"
    (let [req   #(hash-map :request-method :get :headers {"if-modified-since" %})
          last-modified "Sun, 23 Sep 2012 11:00:00 GMT"
          h-resp {:status 200 :headers {"Last-Modified" last-modified} :body ""}
          resp   (not-modified-response h-resp (req last-modified))]
      (is (nil? (:body resp)))
      (is (= (get-in resp [:headers "Content-Length"]) "0"))))
  
  (testing "no modification info"
    (let [response {:status 200 :headers {} :body ""}]
      (is (= 200 (:status (not-modified-response response (etag-request "\"12345\"")))))
      (is (= 200 (:status (not-modified-response response (modified-request "Sun, 23 Sep 2012 10:00:00 GMT")))))))

  (testing "header case insensitivity"
    (let [h-resp {:status 200
                  :headers {"LasT-ModiFied" "Sun, 23 Sep 2012 11:00:00 GMT"
                            "EtAg" "\"123456abcdef\""}}]
      (is (= 304 (:status (not-modified-response
                           h-resp {:request-method :get
                                   :headers {"if-modified-since"
                                             "Sun, 23 Sep 2012 11:00:00 GMT"}}))))
      (is (= 304 (:status (not-modified-response
                           h-resp {:request-method :get
                                   :headers {"if-none-match" "\"123456abcdef\""}}))))))

  (testing "doesn't affect POST, PUT, PATCH or DELETE"
    (let [date "Sat, 22 Sep 2012 11:00:00 GMT"
          req  {:headers {"if-modified-since" date}}
          resp {:status 200 :headers {"Last-Modified" date} :body ""}]
      (are [m s] (= s (:status (not-modified-response resp (assoc req :request-method m))))
        :head   304
        :get    304
        :post   200
        :put    200
        :patch  200
        :delete 200)))

  (testing "only affects 200 OK responses"
    (let [date "Sat, 22 Sep 2012 11:00:00 GMT"
          req  {:request-method :get, :headers {"if-modified-since" date}}
          resp {:headers {"Last-Modified" date} :body ""}]
      (are [s1 s2] (= s2 (:status (not-modified-response (assoc resp :status s1) req)))
        200 304
        302 302
        404 404
        500 500))))

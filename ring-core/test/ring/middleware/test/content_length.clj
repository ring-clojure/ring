(ns ring.middleware.test.content-length
  (:require [clojure.test :refer [deftest is testing]]
            [ring.middleware.content-length :refer [content-length-response
                                                    wrap-content-length]]))

(deftest test-content-length-response
  (testing "strings"
    (is (= "5" (-> (content-length-response
                    {:status 200
                     :headers {"Content-Type" "text/plain; charset=UTF-8"}
                     :body "hello"})
                   (get-in [:headers "Content-Length"])))))
  (testing "strings without a defined charset"
    (is (nil? (-> (content-length-response
                   {:status 200
                    :headers {"Content-Type" "text/plain"}
                    :body "hello"})
                  (get-in [:headers "Content-Length"])))))
  (testing "byte arrays"
    (is (= "11" (-> (content-length-response
                     {:status 200
                      :headers {}
                      :body (.getBytes "hello world" "UTF-8")})
                    (get-in [:headers "Content-Length"])))))
  (testing "files"
    (is (= "6" (-> (content-length-response
                    {:status 200
                     :headers {}
                     :body (java.io.File. "test/ring/assets/plain.txt")})
                   (get-in [:headers "Content-Length"])))))
  (testing "nil body"
    (is (= "0" (-> (content-length-response
                    {:status 200, :headers {}, :body nil})
                   (get-in [:headers "Content-Length"])))))
  (testing "nil response"
    (is (nil? (content-length-response nil))))
  (testing "other data"
    (is (nil? (-> (content-length-response
                   {:status 200
                    :headers {"Content-Type" "text/plain; charset=UTF-8"}
                    :body (list "hello" "world")})
                  (get-in [:headers "Content-Length"])))))
  (testing "manual content-length overrides middleware"
    (is (= "10" (-> (content-length-response
                     {:status 200
                      :headers {"Content-Length" "10"}
                      :body (.getBytes "hello world" "UTF-8")})
                    (get-in [:headers "Content-Length"]))))))

(deftest test-wrap-content-length
  (testing "synchronous handlers"
    (let [handler (wrap-content-length
                   (constantly
                    {:status 200
                     :headers {"Content-Type" "text/plain; charset=UTF-8"}
                     :body "hello"}))]
      (is (= "5" (-> (handler {:request-method :get, :uri "/"})
                     (get-in [:headers "Content-Length"]))))))
  (testing "asynchronous handlers"
    (let [response (promise)
          error    (promise)
          request  {:request-method :get, :url "/"}
          handler  (wrap-content-length
                    (fn [_ respond _]
                      (respond
                       {:status 200
                        :headers {"Content-Type" "text/plain; charset=UTF-8"}
                        :body "hello"})))]
      (handler request response error)
      (is (not (realized? error)))
      (is (realized? response))
      (is (= "5" (get-in @response [:headers "Content-Length"]))))))

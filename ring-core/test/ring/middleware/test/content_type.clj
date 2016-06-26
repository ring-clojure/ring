(ns ring.middleware.test.content-type
  (:require [clojure.test :refer :all]
            [ring.middleware.content-type :refer :all]))

(deftest wrap-content-type-test
  (testing "response without content-type"
    (let [response {:headers {}}
          handler  (wrap-content-type (constantly response))]
      (is (= (handler {:uri "/foo/bar.png"})
             {:headers {"Content-Type" "image/png"}}))
      (is (= (handler {:uri "/foo/bar.txt"})
             {:headers {"Content-Type" "text/plain"}}))))

  (testing "response with content-type"
    (let [response {:headers {"Content-Type" "application/x-foo"}}
          handler (wrap-content-type (constantly response))]
      (is (= (handler {:uri "/foo/bar.png"})
             {:headers {"Content-Type" "application/x-foo"}}))))

  (testing "unknown file extension"
    (let [response {:headers {}}
          handler  (wrap-content-type (constantly response))]
      (is (= (handler {:uri "/foo/bar.xxxaaa"})
             {:headers {"Content-Type" "application/octet-stream"}}))
      (is (= (handler {:uri "/foo/bar"})
             {:headers {"Content-Type" "application/octet-stream"}}))))

  (testing "response with mime-types option"
    (let [response {:headers {}}
          handler  (wrap-content-type (constantly response) {:mime-types {"edn" "application/edn"}})]
      (is (= (handler {:uri "/all.edn"})
             {:headers {"Content-Type" "application/edn"}}))))

  (testing "nil response"
    (let [handler (wrap-content-type (constantly nil))]
      (is (nil? (handler {:uri "/foo/bar.txt"})))))

  (testing "response header case insensitivity"
    (let [response {:headers {"CoNteNt-typE" "application/x-overridden"}}
          handler (wrap-content-type (constantly response))]
      (is (= (handler {:uri "/foo/bar.png"})
             {:headers {"CoNteNt-typE" "application/x-overridden"}})))))

(deftest wrap-content-type-cps-test
  (testing "response without content-type"
    (let [handler  (wrap-content-type (fn [_ cont] (cont {:headers {}})))
          response (promise)]
      (handler {:uri "/foo/bar.png"} response)
      (is (= @response {:headers {"Content-Type" "image/png"}}))))

  (testing "nil response"
    (let [handler  (wrap-content-type (fn [_ cont] (cont nil)))
          response (promise)]
      (handler {:uri "/foo/bar.png"} response)
      (is (nil? @response)))))

(deftest content-type-response-test
  (is (fn? content-type-response)))

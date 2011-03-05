(ns ring.middleware.content-type-test
  (:use clojure.test
        ring.middleware.content-type))

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

  (testing "nil response"
    (let [handler (wrap-content-type (constantly nil))]
      (is (nil? (handler {:uri "/foo/bar.txt"}))))))

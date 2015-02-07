(ns ring.middleware.test.resource
  (:use clojure.test
        [ring.util.io :only (string-input-stream)]
        ring.middleware.resource))

(defn test-handler [request]
  {:status 200
   :headers {}
   :body (string-input-stream "handler")})

(deftest resource-test
  (let [handler (wrap-resource test-handler "/ring/assets" {:gzip true})]
    (are [request body] (= (slurp (:body (handler request))) body)
      {:request-method :get, :uri "/foo.html"}      "foo"
      {:request-method :get, :uri "/index.html"}    "index"
      {:request-method :get, :uri "/bars/foo.html"} "foo"
      {:request-method :get, :uri "/handler"}       "handler"
      {:request-method :post, :uri "/foo.html"}     "handler"
      {:request-method :get, :uri "/pre/foo.html"
       :path-info "/foo.html", :context "/pre"}     "foo"
      {:request-method :get :uri "/gzipped.html"
       :headers {"accept-encoding" "gzip, deflate"}} (slurp "test/ring/assets/gzipped.html.gz")
      {:request-method :get :uri "/gzipped.html"}   "handler")))

(deftest resource-request-test
  (is (fn? resource-request)))

(deftest test-head-request
  (testing "middleware"
    (let [handler  (wrap-resource test-handler "/ring/assets")
          response (handler {:request-method :head, :uri "/foo.html"})]
      (is (= (:status response) 200))
      (is (nil? (:body response)))))
  (testing "request fn"
    (let [request  {:request-method :head, :uri "/foo.html"}
          response (resource-request request "/ring/assets")]
      (is (= (:status response) 200))
      (is (nil? (:body response))))))

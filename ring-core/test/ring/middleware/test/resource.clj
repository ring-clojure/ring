(ns ring.middleware.test.resource
  (:use clojure.test
        [ring.util.io :only (string-input-stream)]
        ring.middleware.resource))

(defn test-handler [request]
  {:status 200
   :headers {}
   :body (string-input-stream "handler")})

(deftest resource-test
  (let [handler (wrap-resource test-handler "/ring/assets")]
    (are [request body] (= (slurp (:body (handler request))) body)
      {:request-method :get, :uri "/foo.html"}      "foo"
      {:request-method :get, :uri "/index.html"}    "index"
      {:request-method :get, :uri "/bars/foo.html"} "foo"
      {:request-method :get, :uri "/handler"}       "handler"
      {:request-method :post, :uri "/foo.html"}     "handler")))

(deftest resource-test-additional-param
  (let [handler (wrap-resource test-handler "/ring/assets" :path-info)]
    (are [request body] (= (slurp (:body (handler request))) body)
      {:request-method :get, :path-info "/foo.html"}      "foo"
      {:request-method :get, :path-info "/index.html"}    "index"
      {:request-method :get, :path-info "/bars/foo.html"} "foo"
      {:request-method :get, :path-info "/handler"}       "handler"
      {:request-method :post, :path-info "/foo.html"}     "handler")))

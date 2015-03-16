(ns ring.middleware.test.resource
  (:import [java.net URL URLClassLoader])
  (:require [clojure.java.io :as io])
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
      {:request-method :post, :uri "/foo.html"}     "handler"
      {:request-method :get, :uri "/pre/foo.html"
       :path-info "/foo.html", :context "/pre"}     "foo")))

(deftest resource-loader-test
  (let [root-loader (->> (Thread/currentThread)
                         .getContextClassLoader
                         (iterate (memfn getParent))
                         (take-while identity)
                         last)
        jarfile     (io/file "test/resource.jar")
        urls        (into-array URL [(.toURL (.toURI jarfile))])
        loader      (URLClassLoader. urls root-loader)
        no-loader   (wrap-resource test-handler "/ring/assets")
        with-loader (wrap-resource test-handler "/ring/assets" {:loader loader})]
    (are [request body] (= (slurp (:body (no-loader request))) body)
         {:request-method :get, :uri "/foo.html"} "foo")
    (are [request body] (= (slurp (:body (with-loader request))) body)
      {:request-method :get, :uri "/foo.html"} "foo-in-jar")))

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

(ns ring.middleware.test.resource
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [ring.middleware.resource :refer :all]
            [ring.util.io :refer [string-input-stream]])
  (:import [java.net URL URLClassLoader]))

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
      {:request-method :get, :uri "/foo.html"} "foo-in-jar")
    (testing "does accept trailing slash in asset path"
      (let [no-loader-s   (wrap-resource test-handler "/ring/assets/")
            with-loader-s (wrap-resource test-handler "/ring/assets/" {:loader loader})]
        (are [request body] (= (slurp (:body (no-loader-s request))) body)
          {:request-method :get, :uri "/foo.html"} "foo")
        (are [request body] (= (slurp (:body (with-loader-s request))) body)
          {:request-method :get, :uri "/foo.html"} "foo-in-jar")))))

(deftest wrap-resource-symlinks-test
  (testing "doesn't follow symlinks by default"
    (let [handler  (wrap-resource test-handler "/ring/assets/bars")
          response (handler {:request-method :get, :uri "/backlink/foo.html"})]
      (is (= (slurp (:body response)) "handler"))))

  (testing "does follow symlinks when option is set"
    (let [handler  (wrap-resource test-handler "/ring/assets/bars" {:allow-symlinks? true})
          response (handler {:request-method :get, :uri "/backlink/foo.html"})]
      (is (= (slurp (:body response)) "foo")))))

(deftest wrap-resource-cps-test
  (let [handler (-> (fn [req respond _] (respond (test-handler req)))
                    (wrap-resource "/ring/assets"))]
    (testing "resource response"
      (let [response  (promise)
            exception (promise)]
        (handler {:request-method :get, :uri "/foo.html"} response exception)
        (is (= "foo" (-> @response :body slurp)))
        (is (not (realized? exception)))))

    (testing "non-resource response"
      (let [response  (promise)
            exception (promise)]
        (handler {:request-method :get, :uri "/handler"} response exception)
        (is (= "handler" (-> @response :body slurp)))
        (is (not (realized? exception)))))))

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

(defn- prefer-foo-handler
  ([request]
   (if (= (:uri request) "/foo.html")
     {:status 200, :headers {}, :body "override"}
     {:status 404, :headers {}, :body "not found"}))
  ([request respond raise]
   (respond (prefer-foo-handler request))))

(deftest test-wrap-resource-with-prefer-handler
  (let [handler (wrap-resource prefer-foo-handler
                               "/ring/assets"
                               {:prefer-handler? true})]

    (testing "middleware serves resource (synchronously)"
      (let [response (handler {:request-method :get, :uri "/index.html"})]
        (is (= 200 (:status response)))
        (is (= "index" (slurp (:body response))))))

    (testing "middleware serves resource (asynchronously)"
      (let [response (promise)
            error    (promise)]
        (handler {:request-method :get, :uri "/index.html"} response error)
        (is (= 200 (:status @response)))
        (is (= "index" (slurp (:body @response))))
        (is (not (realized? error)))))

    (testing "handler serves resource (synchronously)"
      (let [response (handler {:request-method :get, :uri "/foo.html"})]
        (is (= 200 (:status response)))
        (is (= "override" (:body response)))))

    (testing "handler serves resource (asynchronously)"
      (let [response (promise)
            error    (promise)]
        (handler {:request-method :get, :uri "/foo.html"} response error)
        (is (= 200 (:status @response)))
        (is (= "override" (:body @response)))
        (is (not (realized? error)))))

    (testing "404 returned if handler nor resource matches (synchronously)"
      (let [response (handler {:request-method :get, :uri "/unknown.html"})]
        (is (= 404 (:status response)))
        (is (= "not found" (:body response)))))

    (testing "404 returned if handler nor resource matches (asynchronously)"
      (let [response (promise)
            error    (promise)]
        (handler {:request-method :get, :uri "/unknown.html"} response error)
        (is (= 404 (:status @response)))
        (is (= "not found" (:body @response)))
        (is (not (realized? error)))))))

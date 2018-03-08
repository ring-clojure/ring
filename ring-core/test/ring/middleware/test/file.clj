(ns ring.middleware.test.file
  (:require [clojure.test :refer :all]
            [ring.middleware.file :refer :all])
  (:import [java.io File]))

(def test-response
  {:status 200, :headers {}, :body "test"})

(deftest wrap-file-no-directory
  (is (thrown-with-msg? Exception #".*Directory does not exist.*"
                        (wrap-file (constantly test-response) "not_here"))))

(def public-dir "test/ring/assets")
(def index-html (File. ^String public-dir "index.html"))
(def foo-html   (File. ^String public-dir "foo.html"))

(def app (wrap-file (constantly test-response) public-dir))

(deftest test-wrap-file-unsafe-method
  (is (= test-response (app {:request-method :post :uri "/foo"}))))

(deftest test-wrap-file-forbidden-url
  (is (= test-response (app {:request-method :get :uri "/../foo"}))))

(deftest test-wrap-file-no-file
  (is (= test-response (app {:request-method :get :uri "/dynamic"}))))

(deftest test-wrap-file-directory
  (let [{:keys [status headers body]} (app {:request-method :get :uri "/"})]
    (is (= 200 status))
    (is (= (into #{} (keys headers)) #{"Content-Length" "Last-Modified"}))
    (is (= index-html body))))

(deftest test-wrap-file-with-extension
  (let [{:keys [status headers body]} (app {:request-method :get :uri "/foo.html"})]
    (is (= 200 status))
    (is (= (into #{} (keys headers)) #{"Content-Length" "Last-Modified"}))
    (is (= foo-html body))))

(deftest test-wrap-file-no-index
  (let [app  (wrap-file (constantly test-response) public-dir {:index-files? false})
        resp (app {:request-method :get :uri "/"})]
    (is (= test-response resp))))

(deftest test-wrap-file-path-info
  (let [request {:request-method :get
                 :uri "/bar/foo.html"
                 :context "/bar"
                 :path-info "/foo.html"}
        {:keys [status headers body]} (app request)]
    (is (= 200 status))
    (is (= (into #{} (keys headers)) #{"Content-Length" "Last-Modified"}))
    (is (= foo-html body))))

(deftest wrap-file-cps-test
  (let [dynamic-response {:status 200, :headers {}, :body "foo"}
        handler          (-> (fn [_ respond _] (respond dynamic-response))
                             (wrap-file public-dir))]
    (testing "file response"
      (let [request   {:request-method :get :uri "/foo.html"}
            response  (promise)
            exception (promise)]
        (handler request response exception)
        (is (= 200 (:status @response)))
        (is (= #{"Content-Length" "Last-Modified"} (-> @response :headers keys set)))
        (is (= foo-html (:body @response)))
        (is (not (realized? exception)))))

    (testing "non-file response"
      (let [request   {:request-method :get :uri "/dynamic"}
            response  (promise)
            exception (promise)]
        (handler request response exception)
        (is (= dynamic-response @response))
        (is (not (realized? exception)))))))

(deftest file-request-test
  (is (fn? file-request)))

(deftest test-head-request
  (let [{:keys [status headers body]} (app {:request-method :head :uri "/foo.html"})]
    (is (= 200 status))
    (is (= (into #{} (keys headers)) #{"Content-Length" "Last-Modified"}))
    (is (nil? body))))

(deftest test-wrap-file-with-java-io-file
  (let [app (wrap-file (constantly :response) (File. public-dir))]
    (let [{:keys [status headers body]} (app {:request-method :get :uri "/"})]
      (is (= 200 status))
      (is (= (into #{} (keys headers)) #{"Content-Length" "Last-Modified"}))
      (is (= index-html body)))
    (let [{:keys [status headers body]} (app {:request-method :get :uri "/foo.html"})]
      (is (= 200 status))
      (is (= (into #{} (keys headers)) #{"Content-Length" "Last-Modified"}))
      (is (= foo-html body)))))

(defn- prefer-foo-handler
  ([request]
   (if (= (:uri request) "/foo.html")
     {:status 200, :headers {}, :body "override"}
     {:status 404, :headers {}, :body "not found"}))
  ([request respond raise]
   (respond (prefer-foo-handler request))))

(deftest test-wrap-file-with-prefer-handler
  (let [handler (wrap-file prefer-foo-handler
                           (File. public-dir)
                           {:prefer-handler? true})]

    (testing "middleware serves file (synchronously)"
      (let [response (handler {:request-method :get, :uri "/index.html"})]
        (is (= 200 (:status response)))
        (is (= index-html (:body response)))))

    (testing "middleware serves file (asynchronously)"
      (let [response (promise)
            error    (promise)]
        (handler {:request-method :get, :uri "/index.html"} response error)
        (is (= 200 (:status @response)))
        (is (= index-html (:body @response)))
        (is (not (realized? error)))))

    (testing "handler serves file (synchronously)"
      (let [response (handler {:request-method :get, :uri "/foo.html"})]
        (is (= 200 (:status response)))
        (is (= "override" (:body response)))))

    (testing "handler serves file (asynchronously)"
      (let [response (promise)
            error    (promise)]
        (handler {:request-method :get, :uri "/foo.html"} response error)
        (is (= 200 (:status @response)))
        (is (= "override" (:body @response)))
        (is (not (realized? error)))))))

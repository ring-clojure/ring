(ns ring.middleware.test.file
  (:use clojure.test
        ring.middleware.file)
  (:import java.io.File))

(deftest wrap-file-no-directory
  (is (thrown-with-msg? Exception #".*Directory does not exist.*"
    (wrap-file (constantly :response) "not_here"))))

(def public-dir "test/ring/assets")
(def index-html (File. ^String public-dir "index.html"))
(def foo-html   (File. ^String public-dir "foo.html"))

(def app (wrap-file (constantly :response) public-dir))

(deftest test-wrap-file-unsafe-method
  (is (= :response (app {:request-method :post :uri "/foo"}))))

(deftest test-wrap-file-forbidden-url
  (is (= :response (app {:request-method :get :uri "/../foo"}))))

(deftest test-wrap-file-no-file
  (is (= :response (app {:request-method :get :uri "/dynamic"}))))

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
  (let [app  (wrap-file (constantly :response) public-dir {:index-files? false})
        resp (app {:request-method :get :uri "/"})]
    (is (= :response resp))))

(deftest test-wrap-file-path-info
  (let [request {:request-method :get
                 :uri "/bar/foo.html"
                 :context "/bar"
                 :path-info "/foo.html"}
        {:keys [status headers body]} (app request)]
    (is (= 200 status))
    (is (= (into #{} (keys headers)) #{"Content-Length" "Last-Modified"}))
    (is (= foo-html body))))

(deftest file-request-test
  (is (fn? file-request)))

(deftest test-head-request
  (let [{:keys [status headers body]} (app {:request-method :head :uri "/foo.html"})]
    (is (= 200 status))
    (is (= (into #{} (keys headers)) #{"Content-Length" "Last-Modified"}))
    (is (nil? body))))
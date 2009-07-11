(ns ring.middleware.file-test
  (:use (clj-unit core)
        (ring.middleware file))
  (:import (java.io File)))

(deftest "wrap-file: no directory"
  (assert-throws #"Directory does not exist"
    (wrap-file (constantly :response) (File. "not_here"))))

(def public-dir "test/ring/assets")
(def index-html (File. public-dir "index.html"))
(def foo-html   (File. public-dir "foo.html"))

(def app (wrap-file (constantly :response) public-dir))

(deftest "wrap-file: unsafe method"
  (assert= :response (app {:request-method :post :uri "/foo"})))

(deftest "wrap-file: forbidden url"
  (let [{:keys [status body]} (app {:request-method :get :uri "/../foo"})]
    (assert= 403 status)
    (assert-match #"Forbidden" body)))

(deftest "wrap-file: directory"
  (let [{:keys [status headers body]} (app {:request-method :get :uri "/"})]
    (assert= 200 status)
    (assert= {} headers)
    (assert= index-html body)))

(deftest "wrap-file: file without extension"
  (let [{:keys [status headers body]} (app {:request-method :get :uri "/foo"})]
    (assert= 200 status)
    (assert= {} headers)
    (assert= foo-html body)))

(deftest "wrap-file: file with extension"
  (let [{:keys [status headers body]} (app {:request-method :get :uri "/foo.html"})]
    (assert= 200 status)
    (assert= {} headers)
    (assert= foo-html body)))

(deftest "wrap-file: no file"
  (assert= :response (app {:request-method :get :uri "/dynamic"})))

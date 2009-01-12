(ns ring.file-test
  (:use clj-unit.core ring.file)
  (:import java.io.File))

(deftest "wrap: no directory"
  (assert-throws #"Directory does not exist"
    (wrap (File. "not_here") (constantly :response))))

(def public-dir (File. "test/ring/assets"))
(def index-html (File. public-dir "index.html"))
(def foo-html   (File. public-dir "foo.html"))

(def app (wrap public-dir (constantly :response)))

(deftest "wrap: unsafe method"
  (assert= :response (app {:request-method :post :uri "/foo"})))

(deftest "wrap: forbidden url"
  (let [{:keys [status body]} (app {:request-method :get :uri "/../foo"})]
    (assert= 403 status)
    (assert-match #"Forbidden" body)))

(deftest "wrap: directory"
  (let [{:keys [status headers body]} (app {:request-method :get :uri "/"})]
    (assert= 200 status)
    (assert= {} headers)
    (assert= index-html body)))

(deftest "wrap: file without extension"
  (let [{:keys [status headers body]} (app {:request-method :get :uri "/foo"})]
    (assert= 200 status)
    (assert= {} headers)
    (assert= foo-html body)))

(deftest "wrap: file with extension"
  (let [{:keys [status headers body]} (app {:request-method :get :uri "/foo.html"})]
    (assert= 200 status)
    (assert= {} headers)
    (assert= foo-html body)))

(deftest "wrap: no file"
  (assert= :response (app {:request-method :get :uri "/dynamic"})))

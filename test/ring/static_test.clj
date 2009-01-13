(ns ring.static-test
  (:use clj-unit.core ring.static)
  (:import java.io.File))

(def public-dir      (File. "test/ring/assets"))
(def foo-html        (File. "test/ring/assets/foo.html"))
(def nested-foo-html (File. "test/ring/assets/bars/foo.html"))
(def statics ["/foo.html" "/bars/"])

(def app (ring.static/wrap public-dir statics (constantly {:body :dynamic})))

(defn app-response-body [uri]
  (:body (app {:request-method :get :uri uri})))

(deftest "wrap"
  (assert= foo-html        (app-response-body "/foo.html"))
  (assert= nested-foo-html (app-response-body "/bars/foo.html"))
  (assert= :dynamic        (app-response-body "/not/static")))
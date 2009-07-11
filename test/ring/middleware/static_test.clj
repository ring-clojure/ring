(ns ring.middleware.static-test
  (:use (clj-unit core)
        (ring.middleware static))
  (:import (java.io File)))

(def public-dir      (File. "test/ring/assets"))
(def foo-html        (File. "test/ring/assets/foo.html"))
(def nested-foo-html (File. "test/ring/assets/bars/foo.html"))
(def statics ["/foo.html" "/bars/"])

(def app (wrap-static (constantly {:body :dynamic}) public-dir statics))

(defn app-response-body [uri]
  (:body (app {:request-method :get :uri uri})))

(deftest "wrap-static"
  (assert= foo-html        (app-response-body "/foo.html"))
  (assert= nested-foo-html (app-response-body "/bars/foo.html"))
  (assert= :dynamic        (app-response-body "/not/static")))
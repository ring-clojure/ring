(ns ring.middleware.static-test
  (:use (clojure test)
        (ring.middleware static))
  (:import (java.io File)))

(def public-dir      (File. "test/ring/assets"))
(def foo-html        (File. "test/ring/assets/foo.html"))
(def nested-foo-html (File. "test/ring/assets/bars/foo.html"))
(def statics ["/foo.html" "/bars/"])

(def app (wrap-static (constantly {:body :dynamic}) public-dir statics))

(defn app-response-body [uri]
  (:body (app {:request-method :get :uri uri})))

(deftest wrap-static-smoke
  (is (= foo-html        (app-response-body "/foo.html")))
  (is (= nested-foo-html (app-response-body "/bars/foo.html")))
  (is (= :dynamic        (app-response-body "/not/static"))))
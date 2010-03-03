(ns ring.middleware.static-test
  (:use clojure.test
        ring.middleware.static)
  (:import java.io.File))

(def public-dir      "test/ring/assets")
(def foo-html        "test/ring/assets/foo.html")
(def nested-foo-html "test/ring/assets/bars/foo.html")
(def statics ["/foo.html" "/bars/"])

(def app (wrap-static (constantly {:body :dynamic}) public-dir statics))

(defn app-response-body [uri]
  (:body (app {:request-method :get :uri uri})))

(deftest test-wrap-static-smoke
  (is (= (File. foo-html)        (app-response-body "/foo.html")))
  (is (= (File. nested-foo-html) (app-response-body "/bars/foo.html")))
  (is (= :dynamic (app-response-body "/not/static"))))

(ns ring.middleware.test.lint
  (:require [clojure.test :refer :all]
            [ring.middleware.lint :refer :all]
            [ring.util.io :refer [string-input-stream]])
  (:import [java.io File InputStream]))

(def valid-request
  {:server-port        80
   :server-name        "localhost"
   :remote-addr        "192.0.2.235"
   :uri                "/"
   :query-string       nil
   :scheme             :http
   :request-method     :get
   :headers            {}
   :content-type       nil
   :content-length     nil
   :character-encoding nil
   :body               nil})

(def valid-response
  {:status 200 :headers {} :body "valid"})

(def valid-response-app
  (wrap-lint (fn [req] valid-response)))

(defn constant-app
  [constant-response]
  (wrap-lint (fn [req] constant-response)))

(defn is-lint-error [f]
  (is (thrown-with-msg? Exception #"Ring lint error:.*" (f))))

(defmacro lints-req
  [key goods bads]
  (let [good-name (symbol (str "request-" key "-valid-values"))
        bad-name  (symbol (str "request-" key "-invalid-values"))]
    `(do
       (deftest ~good-name
         (doseq [good# ~goods]
           (is (= valid-response
                  (valid-response-app (assoc valid-request ~key good#)))
             (format "%s is a valid value for request key %s"
               (pr-str good#) ~key))))
       (deftest ~bad-name
         (doseq [bad# ~bads]
           (is-lint-error
             (fn [] (valid-response-app (assoc valid-request ~key bad#)))))))))

(defmacro lints-resp
  [key goods bads]
  (let [good-name (symbol (str "response-" key "-valid-values"))
        bad-name  (symbol (str "response-" key "-invalid-values"))]
    `(do
       (deftest ~good-name
         (doseq [good# ~goods]
           (let [response# (assoc valid-response ~key good#)
                 app#      (constant-app response#)]
             (is (= response# (app# valid-request))
               (format "%s is a valid value for response key %s"
                 (pr-str good#) ~key)))))
       (deftest ~bad-name
         (doseq [bad# ~bads]
           (let [response# (assoc valid-response ~key bad#)
                 app#      (constant-app response#)]
             (is-lint-error (fn [] (app# valid-request)))))))))

(lints-req :server-port
  [80 8080]
  [nil "80"])

(lints-req :server-name
  ["localhost" "www.amazon.com" "192.0.2.235"]
  [nil 123])

(lints-req :remote-addr
  ["192.0.2.235" "0:0:0:0:0:0:0:1%0"]
  [nil 123])

(lints-req :uri
  ["/" "/foo" "/foo/bar"]
  [nil ""])

(lints-req :query-string
  [nil "" "foo=bar" "foo=bar&biz=bat"]
  [:foo])

(lints-req :scheme
  [:http :https]
  [nil "http"])

(lints-req :request-method
  [:get :head :options :put :patch :post :delete]
  [nil :FOOBAR "get" 'get])

(lints-req :content-type
  [nil "text/html"]
  [:text/html])

(lints-req :content-length
  [nil 123]
  ["123"])

(lints-req :character-encoding
  [nil "UTF-8"]
  [:utf-8])

(lints-req :ssl-client-cert
  [nil (proxy [java.security.cert.X509Certificate] [] (toString [] "X509Certificate"))]
  ["01234567890ABCDEF"])

(lints-req :headers
  [{"foo" "bar"} {"bat" "Biz"} {"whiz-bang" "high-low"}]
  [nil {:foo "bar"} {"bar" :foo} {"Bar" "foo"}])

(lints-req :body
  [nil (string-input-stream "thebody")]
  ["thebody" :thebody])


(lints-resp :status
  [100 301 500]
  [nil "100" 99])

(lints-resp :headers
  [{} {"foo" "bar"} {"Biz" "Bat"} {"vert" ["high" "low"]} {"horz" #{"left right"}}]
  [nil {:foo "bar"} {"foo" :bar} {"dir" 123}])

(lints-resp :body
  [nil "thebody" (list "foo" "bar") (string-input-stream "thebody") (File. "test/ring/assets/foo.html")]
  [123 :thebody])

(deftest wrap-lint-cps-test
  (testing "valid request and response"
    (let [handler   (wrap-lint (fn [_ respond _] (respond valid-response)))
          response  (promise)
          exception (promise)]
      (handler valid-request response exception)
      (is (= valid-response @response))
      (is (not (realized? exception)))))

  (testing "invalid request"
    (let [handler (wrap-lint (fn [_ respond _] (respond valid-response)))]
      (is-lint-error #(handler {} (fn [_]) (fn [_])))))

  (testing "invalid response"
    (let [handler (wrap-lint (fn [_ respond _] (respond {})))]
      (is-lint-error #(handler valid-request (fn [_]) (fn [_]))))))

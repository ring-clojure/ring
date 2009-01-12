(ns ring.lint-test
  (:use clj-unit.core ring.lint)
  (:import (java.io File InputStream ByteArrayInputStream)))

(defn str-input-stream
  "Returns a ByteArrayInputStream for the given String."
  [string]
  (ByteArrayInputStream. (.getBytes string)))

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
  (wrap (fn [req] valid-response)))

(defn constant-app
  [constant-response]
  (wrap (fn [req] constant-response)))


(defmacro lints-req
  [key goods bads]
  (let [good-name (str "request " key " valid values")
        bad-name  (str "request " key " invalid values")]
    `(do
       (deftest ~good-name
         (doseq [good# ~goods]
           (assert-truth (= valid-response
                           (valid-response-app (assoc valid-request ~key good#)))
             (format "%s is a valid value for request key %s"
               (pr-str good#) ~key))))
       (deftest ~bad-name
         (doseq [bad# ~bads]
           (assert-throws #"Ring lint error:"
             (valid-response-app (assoc valid-request ~key bad#))))))))

(defmacro lints-resp
  [key goods bads]
  (let [good-name (str "response " key " valid values")
        bad-name  (str "response " key " invalid values")]
    `(do
       (deftest ~good-name
         (doseq [good# ~goods]
           (let [response# (assoc valid-response ~key good#)
                 app#      (constant-app response#)]
             (assert-truth (= response# (app# valid-request))
               (format "%s is a valid value for response key %s"
                 (pr-str good#) ~key)))))
       (deftest ~bad-name
         (doseq [bad# ~bads]
           (let [response# (assoc valid-response ~key bad#)
                 app#      (constant-app response#)]
             (assert-throws #"Ring lint error:"
               (app# valid-request))))))))

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
  [:get :head :options :put :post :delete]
  [nil :foobar "get"])

(lints-req :content-type
  [nil "text/html"]
  [:text/html])

(lints-req :content-length
  [nil 123]
  ["123"])

(lints-req :character-encoding
  [nil "UTF-8"]
  [:utf-8])

(lints-req :headers
  [{"foo" "bar"} {"bat" "Biz"} {"whiz-bang" "high-low"}]
  [nil {:foo "bar"} {"bar" :foo} {"Bar" "foo"}])

(lints-req :body
  [nil (str-input-stream "thebody")]
  ["thebody" :thebody])


(lints-resp :status
  [100 301 500]
  [nil "100" 99])

(lints-resp :headers
  [{} {"foo" "bar"} {"Biz" "Bat"} {"vert" ["high" "low"]} {"horz" #{"left right"}}]
  [nil {:foo "bar"} {"foo" :bar} {"dir" 123}])

(lints-resp :body
  [nil "thebody" (str-input-stream "thebody") (File. "test/ring/assets/foo.html")]
  [123 :thebody])

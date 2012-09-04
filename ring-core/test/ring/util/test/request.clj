(ns ring.util.test.request
  (:use clojure.test
        ring.util.request))

(def base-req
  {:server-port 80
   :server-name "example.com"
   :uri "/"
   :query-string nil
   :scheme :http})

(deftest base-url-test
  (are [req url] (= (base-url req) url)
       base-req "http://example.com/"
       (assoc base-req :server-port 8080) "http://example.com:8080/"
       (assoc base-req :uri "/abc/def") "http://example.com/"
       (assoc base-req :query-string "a=b&c=d") "http://example.com/"
       (assoc base-req :scheme :https) "https://example.com/"))

(deftest request-url-test
  (are [req url] (= (request-url req) url)
       base-req "http://example.com/"
       (assoc base-req :server-port 8080) "http://example.com:8080/"
       (assoc base-req :uri "/abc/def") "http://example.com/abc/def"
       (assoc base-req :query-string "a=b&c=d") "http://example.com/?a=b&c=d"
       (assoc base-req :scheme :https) "https://example.com/"))

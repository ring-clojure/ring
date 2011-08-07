(ns ring.middleware.test.cookies
  (:use clojure.test
        ring.middleware.cookies))

(deftest wrap-cookies-basic-cookie
  (let [req  {:headers {"cookie" "a=b"}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {"a" {:value "b"}} resp))))

(deftest wrap-cookies-multiple-cookies
  (let [req  {:headers {"cookie" "a=b; c=d,e=f"}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {"a" {:value "b"}, "c" {:value "d"}, "e" {:value "f"}}
           resp))))

(deftest wrap-cookies-quoted-cookies
  (let [req  {:headers {"cookie" "a=\"b=c;e=f\""}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {"a" {:value "b=c;e=f"}}
           resp))))

(deftest wrap-cookies-escaped-quotes
  (let [req  {:headers {"cookie" "a=\"\\\"b\\\"\""}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {"a" {:value "\"b\""}}
           resp))))

(deftest wrap-cookies-extra-attrs
  (let [req  {:headers {"cookie" "a=b;$Path=\"/\";$Domain=localhost"}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {"a" {:value "b", :path "/", :domain "localhost"}}
           resp))))

(deftest wrap-cookies-set-basic-cookie
  (let [handler (constantly {:cookies {"a" "b"}})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" (list "a=b")}
           (:headers resp)))))

(deftest wrap-cookies-set-multiple-cookies
  (let [handler (constantly {:cookies {"a" "b", "c" "d"}})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" (list "a=b" "c=d")}
           (:headers resp)))))

(deftest wrap-cookies-set-keyword-cookie
  (let [handler (constantly {:cookies {:a "b"}})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" (list "a=b")}
           (:headers resp)))))

(deftest wrap-cookies-set-extra-attrs
  (let [cookies {"a" {:value "b", :path "/", :secure true, :http-only true }}
        handler (constantly {:cookies cookies})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" (list "a=b;Path=/;Secure;HttpOnly")}
           (:headers resp)))))

(deftest wrap-cookies-always-assocs-map
  (let [req  {:headers {}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {} resp))))

(deftest wrap-cookies-read-urlencoded
  (let [req  {:headers {"cookie" "a=hello+world"}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {"a" {:value "hello world"}} resp))))

(deftest wrap-cookies-set-urlencoded-cookie
  (let [handler (constantly {:cookies {"a" "hello world"}})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" (list "a=hello+world")}
           (:headers resp)))))

(deftest wrap-cookies-keep-set-cookies-intact
  (let [handler (constantly {:headers {"Set-Cookie" (list "a=b")}
                             :cookies {:c "d"}})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" (list "a=b" "c=d")}
           (:headers resp)))))

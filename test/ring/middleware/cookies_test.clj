(ns ring.middleware.cookies-test
  (:use clj-unit.core
        ring.middleware.cookies))

(deftest "wrap-cookies: basic cookie"
  (let [req  {:headers {"cookie" "a=b"}}
        resp ((wrap-cookies :cookies) req)]
    (assert= {"a" {:value "b"}} resp)))

(deftest "wrap-cookies: multiple cookies"
  (let [req  {:headers {"cookie" "a=b; c=d,e=f"}}
        resp ((wrap-cookies :cookies) req)]
    (assert= {"a" {:value "b"}, "c" {:value "d"}, "e" {:value "f"}}
             resp)))

(deftest "wrap-cookies: quoted cookies"
  (let [req  {:headers {"cookie" "a=\"b=c;e=f\""}}
        resp ((wrap-cookies :cookies) req)]
    (assert= {"a" {:value "b=c;e=f"}}
             resp)))

(deftest "wrap-cookies: escaped quotes"
  (let [req  {:headers {"cookie" "a=\"\\\"b\\\"\""}}
        resp ((wrap-cookies :cookies) req)]
    (assert= {"a" {:value "\"b\""}}
             resp)))

(deftest "wrap-cookies: extra attrs"
  (let [req  {:headers {"cookie" "a=b;$Path=\"/\";$Domain=localhost"}}
        resp ((wrap-cookies :cookies) req)]
    (assert= {"a" {:value "b", :path "/", :domain "localhost"}}
             resp)))

(deftest "wrap-cookies: set basic cookie"
  (let [handler (constantly {:cookies {"a" "b"}})
        resp    ((wrap-cookies handler) {})]
    (assert= {"Set-Cookie" (list "a=\"b\"")}
             (:headers resp))))

(deftest "wrap-cookies: set multiple cookies"
  (let [handler (constantly {:cookies {"a" "b", "c" "d"}})
        resp    ((wrap-cookies handler) {})]
    (assert= {"Set-Cookie" (list "a=\"b\"" "c=\"d\"")}
             (:headers resp))))

(deftest "wrap-cookies: set keyword cookie"
  (let [handler (constantly {:cookies {:a "b"}})
        resp    ((wrap-cookies handler) {})]
    (assert= {"Set-Cookie" (list "a=\"b\"")}
             (:headers resp))))

(deftest "wrap-cookies: set extra attrs"
  (let [cookies {"a" {:value "b", :path "/", :secure true}}
        handler (constantly {:cookies cookies})
        resp    ((wrap-cookies handler) {})]
    (assert= {"Set-Cookie" (list "a=\"b\";Path=\"/\";Secure")}
             (:headers resp))))

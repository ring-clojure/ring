(ns ring.middleware.test.cookies
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [ring.middleware.cookies :refer :all]
            [clj-time.core :refer [date-time interval]]))

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
  (let [req  {:headers {"cookie" "a=\"b\""}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {"a" {:value "b"}}
           resp))))

(deftest wrap-cookies-quoted-cookies-no-urlencode
  (let [req  {:headers {"cookie" "a=U3VwIFdpenpvcmxkCg%3D%3D"}}
        resp ((wrap-cookies :cookies {:decoder identity}) req)]
    (is (= {"a" {:value "U3VwIFdpenpvcmxkCg%3D%3D"}}
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

(defn- split-set-cookie [headers]
  (letfn [(split-header [v] (set (mapcat #(str/split % #";") v)))]
    (update-in headers ["Set-Cookie"] split-header)))

(deftest wrap-cookies-set-extra-attrs
  (let [cookies {"a" {:value "b", :path "/", :secure true, :http-only true }}
        handler (constantly {:cookies cookies})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" #{"a=b" "Path=/" "Secure" "HttpOnly"}}
           (split-set-cookie (:headers resp))))))

(deftest wrap-cookies-always-assocs-map
  (let [req  {:headers {}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {} resp))))

(deftest wrap-cookies-read-urlencoded
  (let [req  {:headers {"cookie" "a=hello+world"}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {"a" {:value "hello world"}} resp))))

(deftest wrap-cookies-no-urlencode
  (let [req {:headers {"cookie" "a=hello+world"}}
        resp ((wrap-cookies :cookies {:decoder identity}) req)]
    (is (= {"a" {:value "hello+world"}} resp))))

(deftest wrap-cookies-no-urlencode-base64
  (let [req {:headers {"cookie" "a=U3VwIFdpenpvcmxkCg=="}}
        resp ((wrap-cookies :cookies {:decoder identity}) req)]
    (is (= {"a" {:value "U3VwIFdpenpvcmxkCg=="}} resp))))

(deftest wrap-cookies-set-urlencoded-cookie
  (let [handler (constantly {:cookies {"a" "hello world"}})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" (list "a=hello+world")}
           (:headers resp)))))

(deftest wrap-cookies-set-no-urlencoded-cookie
  (let [handler (constantly {:cookies {"a" "hello world"}})
        resp    ((wrap-cookies handler {:encoder
                                        (fn [m]
                                          (apply format "%s=%s" (first (seq m))))}) {})]
    (is (= {"Set-Cookie" (list "a=hello world")}
           (:headers resp)))))

(deftest wrap-cookies-invalid-url-encoded
  (let [req  {:headers {"cookie" "a=%D"}}
        resp ((wrap-cookies :cookies) req)]
    (is (= {} resp))))

(deftest wrap-cookies-keep-set-cookies-intact
  (let [handler (constantly {:headers {"Set-Cookie" (list "a=b")}
                             :cookies {:c "d"}})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" (list "a=b" "c=d")}
           (:headers resp)))))

(deftest wrap-cookies-invalid-attrs
  (let [response {:cookies {"a" {:value "foo" :invalid true}}}
        handler  (wrap-cookies (constantly response))]
    (is (thrown? AssertionError (handler {})))))

(deftest wrap-cookies-accepts-max-age
  (let [cookies {"a" {:value "b", :path "/",
                      :secure true, :http-only true,
                      :max-age 123}}
        handler (constantly {:cookies cookies})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" #{"a=b" "Path=/" "Secure" "HttpOnly" "Max-Age=123"}}
           (split-set-cookie (:headers resp))))))

(deftest wrap-cookies-accepts-same-site
  (testing "Allows :strict and :lax values"
    (let [cookies {"a" {:value "b" :path "/" :same-site :strict}}
          handler (constantly {:cookies cookies})
          resp    ((wrap-cookies handler) {})]
      (is (= {"Set-Cookie" #{"a=b" "Path=/" "SameSite=Strict"}}
             (split-set-cookie (:headers resp)))))

    (let [cookies {"a" {:value "b" :path "/" :same-site :lax}}
          handler (constantly {:cookies cookies})
          resp    ((wrap-cookies handler) {})]
      (is (= {"Set-Cookie" #{"a=b" "Path=/" "SameSite=Lax"}}
             (split-set-cookie (:headers resp))))))

  (testing "Disallows other values"
    (let [cookies {"a" {:value "b" :path "/" :same-site :loose}}
          handler (constantly {:cookies cookies})]
      (is (thrown? AssertionError ((wrap-cookies handler) {}))))))

(deftest wrap-cookies-accepts-expires
  (let [cookies {"a" {:value "b", :path "/",
                      :secure true, :http-only true,
                      :expires "123"}}
        handler (constantly {:cookies cookies})
        resp    ((wrap-cookies handler) {})]
    (is (= {"Set-Cookie" #{"a=b" "Path=/" "Secure" "HttpOnly" "Expires=123"}}
           (split-set-cookie (:headers resp))))))

(deftest wrap-cookies-accepts-max-age-from-clj-time
  (let [cookies {"a" {:value "b", :path "/",
                      :secure true, :http-only true,
                      :max-age (interval (date-time 2012)
                                         (date-time 2015))}}
        handler (constantly {:cookies cookies})
        resp    ((wrap-cookies handler) {})
        max-age 94694400]
    (is (= {"Set-Cookie" #{"a=b" "Path=/" "Secure" "HttpOnly" (str "Max-Age=" max-age)}}
           (split-set-cookie (:headers resp))))))

(deftest wrap-cookies-accepts-expires-from-clj-time
  (let [cookies {"a" {:value "b", :path "/",
                      :secure true, :http-only true,
                      :expires (date-time 2015 12 31)}}
        handler (constantly {:cookies cookies})
        resp    ((wrap-cookies handler) {})
        expires "Thu, 31 Dec 2015 00:00:00 +0000"]
    (is (= {"Set-Cookie" #{"a=b" "Path=/" "Secure" "HttpOnly" (str "Expires=" expires)}}
           (split-set-cookie (:headers resp))))))

(deftest wrap-cookies-accepts-expires-from-clj-time-in-non-us-locale
  (let [default-locale (java.util.Locale/getDefault)]
    (try
      (java.util.Locale/setDefault java.util.Locale/FRANCE)
      (let [cookies {"a" {:value "b", :path "/",
                          :secure true, :http-only true,
                          :expires (date-time 2015 12 31)}}
            handler (constantly {:cookies cookies})
            resp    ((wrap-cookies handler) {})
            expires "Thu, 31 Dec 2015 00:00:00 +0000"]
        (is (= {"Set-Cookie" #{"a=b" "Path=/" "Secure" "HttpOnly" (str "Expires=" expires)}}
          (split-set-cookie (:headers resp)))))
      (finally
        (java.util.Locale/setDefault default-locale)))))

(deftest wrap-cookies-throws-exception-when-not-using-intervals-correctly
  (let [cookies {"a" {:value "b", :path "/",
                      :secure true, :http-only true,
                      :expires (interval (date-time 2012)
                                         (date-time 2015))}}
        handler (constantly {:cookies cookies})]
    (is (thrown? AssertionError ((wrap-cookies handler) {})))))

(deftest wrap-cookies-throws-exception-when-not-using-datetime-correctly
  (let [cookies {"a" {:value "b", :path "/",
                      :secure true, :http-only true,
                      :max-age (date-time 2015 12 31)}}
        handler (constantly {:cookies cookies})]
    (is (thrown? AssertionError ((wrap-cookies handler) {})))))

(deftest parse-cookies-on-request-basic-cookie
  (let [req {:headers {"cookie" "a=b"}}]
    (is (= {"a" {:value "b"}}
           ((cookies-request req) :cookies)))))

(deftest parse-cookies-on-request-multiple-cookies
  (let [req {:headers {"cookie" "a=b; c=d,e=f"}}]
    (is (= {"a" {:value "b"}, "c" {:value "d"}, "e" {:value "f"}}
           ((cookies-request req) :cookies)))))

(deftest parse-cookies-url-encoding
  (let [req {:headers {"cookie" "a=%22/test%22"}}]
    (is (= {"a" {:value "\"/test\""}} ((cookies-request req) :cookies)))))

(deftest wrap-cookies-cps-test
  (testing "read cookie"
    (let [handler   (wrap-cookies (fn [req respond _] (respond (:cookies req))))
          request   {:headers {"cookie" "a=b"}}
          response  (promise)
          exception (promise)]
      (handler request response exception)
      (is (= {"a" {:value "b"}} @response))
      (is (not (realized? exception)))))

  (testing "write cookie"
    (let [handler   (wrap-cookies (fn [_ respond _] (respond {:cookies {"a" "b"}})))
          response  (promise)
          exception (promise)]
      (handler {} response exception)
      (is (= {"Set-Cookie" (list "a=b")} (:headers @response)))
      (is (not (realized? exception))))))

(deftest cookies-response-test
  (is (fn? cookies-response)))

(deftest cookies-request-test
  (is (fn? cookies-request)))

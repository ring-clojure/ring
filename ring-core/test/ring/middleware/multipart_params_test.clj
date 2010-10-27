(ns ring.middleware.multipart-params-test
  (:use clojure.test
        ring.middleware.multipart-params)
  (:require [ring.util.test :as tu])
  (:import java.io.File))

(def ^{:private true}
  upload-content-type
  "multipart/form-data; boundary=----WebKitFormBoundaryAyGUY6aMxOI6UF5s")

(def ^{:private true}
  upload-content-length
  188)

(def ^{:private true}
  upload-body
  (tu/string-input-stream
    "------WebKitFormBoundaryAyGUY6aMxOI6UF5s\r\nContent-Disposition: form-data; name=\"upload\"; filename=\"test.txt\"\r\nContent-Type: text/plain\r\n\r\nfoo\r\n\r\n------WebKitFormBoundaryAyGUY6aMxOI6UF5s--"))

(def ^{:private true}
  wrapped-echo
  (wrap-multipart-params identity))

(deftest test-wrap-multipart-params
  (let [req {:content-type   upload-content-type
             :content-length upload-content-length
             :body           upload-body
             :params         {"foo" "bar"}}
        resp (wrapped-echo req)]
    (is (= "bar" (get-in resp [:params "foo"])))
    (let [upload (get-in resp [:params "upload"])]
      (is (= "test.txt" (:filename upload)))
      (is (= 5 (:size upload)))
      (is (= "text/plain" (:content-type upload)))
      (is (instance? File (:tempfile upload)))
      (is (= "foo\r\n" (slurp (:tempfile upload)))))))

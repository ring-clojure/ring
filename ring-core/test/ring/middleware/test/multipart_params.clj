(ns ring.middleware.test.multipart-params
  (:use clojure.test
        ring.middleware.multipart-params
        [ring.util.io :only (string-input-stream)])
  (:import java.io.InputStream))

(defn string-store [item]
  (-> (select-keys item [:filename :content-type])
      (assoc :content (slurp (:stream item)))))

(deftest test-wrap-multipart-params
  (let [form-body (str "--XXXX\r\n"
                       "Content-Disposition: form-data;"
                       "name=\"upload\"; filename=\"test.txt\"\r\n"
                       "Content-Type: text/plain\r\n\r\n"
                       "foo\r\n"
                       "--XXXX\r\n"
                       "Content-Disposition: form-data;"
                       "name=\"baz\"\r\n\r\n"
                       "qux\r\n"
                       "--XXXX--")
        handler (wrap-multipart-params identity {:store string-store})
        request {:content-type "multipart/form-data; boundary=XXXX"
                 :content-length (count form-body)
                 :params {"foo" "bar"}
                 :body (string-input-stream form-body)}
        response (handler request)]
    (is (= (get-in response [:params "foo"]) "bar"))
    (is (= (get-in response [:params "baz"]) "qux"))
    (let [upload (get-in response [:params "upload"])]
      (is (= (:filename upload)     "test.txt"))
      (is (= (:content-type upload) "text/plain"))
      (is (= (:content upload)      "foo")))))

(deftest test-multiple-params
  (let [form-body (str "--XXXX\r\n"
                       "Content-Disposition: form-data;"
                       "name=\"foo\"\r\n\r\n"
                       "bar\r\n"
                       "--XXXX\r\n"
                       "Content-Disposition: form-data;"
                       "name=\"foo\"\r\n\r\n"
                       "baz\r\n"
                       "--XXXX--")
        handler (wrap-multipart-params identity {:store string-store})
        request {:content-type "multipart/form-data; boundary=XXXX"
                 :content-length (count form-body)
                 :body (string-input-stream form-body)}
        response (handler request)]
    (is (= (get-in response [:params "foo"])
           ["bar" "baz"]))))

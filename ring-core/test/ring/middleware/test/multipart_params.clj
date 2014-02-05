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
        request {:headers {"content-type" "multipart/form-data; boundary=XXXX"
                           "content-length" (str (count form-body))}
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
        request {:headers {"content-type" "multipart/form-data; boundary=XXXX"
                           "content-length" (str (count form-body))}
                 :body (string-input-stream form-body)}
        response (handler request)]
    (is (= (get-in response [:params "foo"])
           ["bar" "baz"]))))

(defn all-threads []
  (.keySet (Thread/getAllStackTraces)))

(deftest test-multipart-threads
  (testing "no thread leakage when handler called"
    (let [handler (wrap-multipart-params identity)]
      (dotimes [_ 200]
        (handler {}))
      (is (< (count (all-threads))
             100))))

  (testing "no thread leakage from default store"
    (let [form-body (str "--XXXX\r\n"
                         "Content-Disposition: form-data;"
                         "name=\"upload\"; filename=\"test.txt\"\r\n"
                         "Content-Type: text/plain\r\n\r\n"
                         "foo\r\n"
                         "--XXXX--")]
      (dotimes [_ 200]
        (let [handler (wrap-multipart-params identity)
              request {:headers {"content-type" "multipart/form-data; boundary=XXXX"
                                 "content-length" (str (count form-body))}
                       :body (string-input-stream form-body)}]
          (handler request))))
    (is (< (count (all-threads))
           100))))

(defn- form-body-with-size [sz]
  {:pre [(> sz 113)]}
  (str "--XXXX\r\n"
       "Content-Disposition: form-data;"
       "name=\"upload\"; filename=\"test.txt\"\r\n"
       "Content-Type: text/plain\r\n\r\n"
       (apply str (repeat (- sz 113) "1")) "\r\n"
       "--XXXX--"))

(defn- request-of [form-body]
  {:content-type "multipart/form-data; boundary=XXXX"
   :content-length (count form-body)
   :body (string-input-stream form-body)})

(defn- root-cause [e]
  (let [cause (.getCause e)]
    (if (and cause (not (= java.io.IOException (type cause))))
      (recur cause)
      e)))

(deftest test-max-size-settings
  (testing "Respects max request size in bytes"
    (let [handler (wrap-multipart-params identity {:max-request-size 300, :store string-store})]

      (let [response (handler (request-of (form-body-with-size 300)))]
        (is (= (get-in response [:params "upload" :filename]) "test.txt")))

      (is (thrown? org.apache.commons.fileupload.FileUploadBase$SizeLimitExceededException
                   (handler (request-of (form-body-with-size 500)))))))

  (testing "Respects max file size in bytes"
    (let [handler (wrap-multipart-params identity {:max-file-size 300, :store string-store})]

      ; Actual file size is less than 300
      (let [response (handler (request-of (form-body-with-size 400)))]
        (is (= (get-in response [:params "upload" :filename]) "test.txt")))

      (try
        (handler (request-of (form-body-with-size 500)))
        (is false "Should fail with exception!")
        (catch Exception e
          (is (instance? org.apache.commons.fileupload.FileUploadBase$FileSizeLimitExceededException
                         (root-cause e))))))))

(deftest multipart-params-request-test
  (is (fn? multipart-params-request)))

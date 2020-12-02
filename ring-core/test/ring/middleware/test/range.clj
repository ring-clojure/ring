(ns ring.middleware.test.range
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [ring.core.protocols :refer :all]
            [ring.middleware.range :refer :all]
            [ring.util.io :refer [string-input-stream]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream File]))

(def public-dir "test/ring/assets/")
(def binary-file-with-all-possible-bytes (File. ^String public-dir "binary-file-with-all-possible-bytes.bin"))

(defn run-range-middleware-and-convert-body-to-str
  ([response request] (run-range-middleware-and-convert-body-to-str response request {}))
  ([response request opts]
   (run-range-middleware-and-convert-body-to-str response request opts (ByteArrayOutputStream.)))
  ([response request opts byte-stream]
   (let [handler (wrap-range-header (constantly response) opts)
         result (handler request)]
     (if-let [body (:body result)]
       (do (write-body-to-stream body response byte-stream)
           (assoc result :body (.toString byte-stream)))
       result))))

(deftest test-boundary-generator
  (let [boundary (boundary-generator)]
    (is (string? boundary))
    (is (not (empty? boundary)))
    (is (some? (re-find #"[a-zA-Z0-9]+" boundary))))

  (testing "is random"
    (is (not= (boundary-generator) (boundary-generator)))))

(deftest wrap-range-header-single-range-test
  (let [response {:status 200
                  :body "a very long response with a lot of bytes"}
        good-request {:request-method :get
                      :headers {"Range" "bytes=2-10"}}]
    (testing "it works with strings"
      (is (= (run-range-middleware-and-convert-body-to-str
               response
               good-request)
             {:body "very long"
              :headers {"Accept-Ranges" "bytes"
                        "Content-Length" "9"
                        "Content-Range" "bytes 2-10/40"}
              :status 206})))

    (testing "it works with files"
      (is (= (run-range-middleware-and-convert-body-to-str
               (assoc response :body binary-file-with-all-possible-bytes)
               (assoc-in good-request [:headers "Range"] "bytes=-50"))
             {:body (->> (range 206 256) (map unchecked-byte) (byte-array) (String.))
              :headers {"Accept-Ranges" "bytes"
                        "Content-Length" "50"
                        "Content-Range" "bytes 206-255/256"}
              :status 206})))

    (testing "it works with input streams"
      (is (= (run-range-middleware-and-convert-body-to-str
               (assoc response :body (string-input-stream "some string in a stream"))
               (assoc-in good-request [:headers "Range"] "bytes=5-"))
             {:body "string in a stream"
              :headers {"Accept-Ranges" "bytes"
                        "Content-Length" "18"
                        "Content-Range" "bytes 5-22/23"}
              :status 206})))

    (testing "header does not parse"
      (is (= (run-range-middleware-and-convert-body-to-str
               response
               (assoc-in good-request [:headers "Range"] "bytes=a"))
             {:body nil
              :headers {"Accept-Ranges" "bytes"
                        "Content-Range" "bytes */40"}
              :status 416}))
      (is (= (run-range-middleware-and-convert-body-to-str
               response
               (assoc-in good-request [:headers "Range"] "bytes=0x1337"))
             {:body nil
              :headers {"Accept-Ranges" "bytes"
                        "Content-Range" "bytes */40"}
              :status 416})))

    (testing "header parses but out of range"
      (is (= (run-range-middleware-and-convert-body-to-str
               response
               (assoc-in good-request [:headers "Range"] "bytes=500-9001"))
             {:body nil
              :headers {"Accept-Ranges" "bytes"
                        "Content-Range" "bytes */40"}
              :status 416})))

    (testing "header has overlapping ranges with content length known"
      (is (= (run-range-middleware-and-convert-body-to-str
               response
               (assoc-in good-request [:headers "Range"] "bytes=1-39, -35"))
             {:body nil
              :headers {"Accept-Ranges" "bytes"
                        "Content-Range" "bytes */40"}
              :status 416})))

    (testing "response status must be 200"
      (is (= (run-range-middleware-and-convert-body-to-str
               (assoc response :status 304)
               good-request)
             {:body "a very long response with a lot of bytes"
              :headers {"Accept-Ranges" "bytes"}
              :status 304})))

    (testing "request method must be GET"
      (is (= (run-range-middleware-and-convert-body-to-str
               response
               (assoc good-request :request-method :post))
             {:body "a very long response with a lot of bytes"
              :headers {"Accept-Ranges" "bytes"}
              :status 200})))))

(deftest wrap-range-header-multiple-ranges-test
  (testing "range must be valid"
    (let [response {:status 200
                    :body "a body with ASCII characters only"}
          handler (wrap-range-header (constantly response))]
      (is (= (handler {:request-method :get
                       :headers {"Range" "bytes=ab39"}})
             {:body nil
              :headers {"Accept-Ranges" "bytes"
                        "Content-Range" "bytes */33"}
              :status 416}))
      (is (= (handler {:request-method :get
                       :headers {"Range" "bytes=47"}})
             {:body nil
              :headers {"Accept-Ranges" "bytes"
                        "Content-Range" "bytes */33"}
              :status 416}))
      (is (= (handler {:request-method :get
                       :headers {"Range" "bytes=350-"}})
             {:body nil
              :headers {"Accept-Ranges" "bytes"
                        "Content-Range" "bytes */33"}
              :status 416}))))

  (testing "response must be 200"
    (let [response {:status 200
                    :body "a body with ASCII characters only"}
          handler-304 (wrap-range-header (constantly (assoc response :status 304)))
          handler-201 (wrap-range-header (constantly (assoc response :status 201)))
          handler-200 (wrap-range-header (constantly (assoc response :status 200)))]
      (is (= (handler-304 {:request-method :get
                           :headers {"Range" "bytes=2-3"}})
             (-> response
                 (assoc-in [:headers "Accept-Ranges"] "bytes")
                 (assoc :status 304))))
      (is (= (handler-201 {:request-method :get
                           :headers {"Range" "bytes=2-3"}})
             (-> response
                 (assoc-in [:headers "Accept-Ranges"] "bytes")
                 (assoc :status 201))))
      (is (not= (handler-200 {:request-method :get
                              :headers {"Range" "bytes=2-3"}})
                (-> response
                    (assoc-in [:headers "Accept-Ranges"] "bytes")
                    (assoc :status 200))))))

  (testing "string"
    (let [ascii-response {:status 200
                          :body "a body with ASCII characters only"}]
      (is (= (run-range-middleware-and-convert-body-to-str ascii-response
                                                           {:request-method :get
                                                            :uri "some-file.txt"
                                                            :headers {"Range" "bytes=3-5,7-10"}}
                                                           {:boundary-generator-fn (constantly "aboundary")})
             {:body (str/join "\r\n" ["--aboundary"
                                      "Content-Type: text/plain"
                                      "Content-Range: bytes 3-5/33"
                                      ""
                                      "ody"
                                      "--aboundary"
                                      "Content-Type: text/plain"
                                      "Content-Range: bytes 7-10/33"
                                      ""
                                      "with"
                                      "--aboundary--"])
              :headers {"Accept-Ranges" "bytes"
                        "Content-Length" "165"
                        "Content-Type" "multipart/byteranges; boundary=aboundary"}
              :status 206}))))

  (testing "file"
    (is (= (run-range-middleware-and-convert-body-to-str
             {:status 200
              :body binary-file-with-all-possible-bytes}
             {:request-method :get
              :uri "some-video.mp4"
              :headers {"Range" "bytes=7-20,  59-102,-13"}}
             {:boundary-generator-fn (constantly "Mp4Boundary34")})
           {:body (str/join "\r\n" ["--Mp4Boundary34"
                                    "Content-Type: video/mp4"
                                    "Content-Range: bytes 7-20/256"
                                    ""
                                    (->> (range 7 21) (byte-array) (String.))
                                    "--Mp4Boundary34"
                                    "Content-Type: video/mp4"
                                    "Content-Range: bytes 59-102/256"
                                    ""
                                    (->> (range 59 103) (byte-array) (String.))
                                    "--Mp4Boundary34"
                                    "Content-Type: video/mp4"
                                    "Content-Range: bytes 243-255/256"
                                    ""
                                    (->> (range 243 256) (byte-array) (String.))
                                    "--Mp4Boundary34--"])
            :headers {"Accept-Ranges" "bytes"
                      "Content-Length" "324"
                      "Content-Type" "multipart/byteranges; boundary=Mp4Boundary34"}
            :status 206})))

  (testing "InputStream"
    (is (= (run-range-middleware-and-convert-body-to-str
             {:status 200
              :body (string-input-stream "another string example")}
             {:request-method :get
              :uri "foo.mpeg"
              :headers {"Range" "bytes=4-5,-6"}}
             {:boundary-generator-fn (constantly "baaz")})
           {:body (str/join "\r\n" ["--baaz"
                                    "Content-Type: video/mpeg"
                                    "Content-Range: bytes 4-5/22"
                                    ""
                                    "he"
                                    "--baaz"
                                    "Content-Type: video/mpeg"
                                    "Content-Range: bytes 16-21/22"
                                    ""
                                    "xample"
                                    "--baaz--"])
            :headers {"Accept-Ranges" "bytes"
                      "Content-Length" "152"
                      "Content-Type" "multipart/byteranges; boundary=baaz"}
            :status 206})))

  (testing "seq of strings"
    (is (= (run-range-middleware-and-convert-body-to-str
             {:status 200
              :body (seq ["part 1\r\n" "string 2" "abc789"])}
             {:request-method :get
              :uri "foo.png"
              :headers {"Range" "bytes=0-0, 3-3, 6-7, -7"}}
             {:boundary-generator-fn (constantly "somerandomstring")})
           {:body (str/join "\r\n" ["--somerandomstring"
                                    "Content-Type: image/png"
                                    "Content-Range: bytes 0-0/22"
                                    ""
                                    "p"
                                    "--somerandomstring"
                                    "Content-Type: image/png"
                                    "Content-Range: bytes 3-3/22"
                                    ""
                                    "t"
                                    "--somerandomstring"
                                    "Content-Type: image/png"
                                    "Content-Range: bytes 6-7/22"
                                    ""
                                    "\r\n"
                                    "--somerandomstring"
                                    "Content-Type: image/png"
                                    "Content-Range: bytes 15-21/22"
                                    ""
                                    "2abc789"
                                    "--somerandomstring--"])
            :headers {"Accept-Ranges" "bytes"
                      "Content-Length" "345"
                      "Content-Type" "multipart/byteranges; boundary=somerandomstring"}
            :status 206}))))

(deftest wrap-range-header-uses-existing-content-type-header-test
  (is (= (run-range-middleware-and-convert-body-to-str
           {:status 200
            :body binary-file-with-all-possible-bytes
            :headers {"Content-Type" "image/jpeg"}}
           {:request-method :get
            :uri "some-video.gif"
            :headers {"Range" "bytes=3-5, 12-"}}
           {:boundary-generator-fn (constantly "SomeBoundary")})
         {:body (str/join "\r\n" ["--SomeBoundary"
                                 "Content-Type: image/jpeg"
                                 "Content-Range: bytes 3-5/256"
                                 ""
                                 (->> (range 3 6) (byte-array) (String.))
                                 "--SomeBoundary"
                                 "Content-Type: image/jpeg"
                                 "Content-Range: bytes 12-255/256"
                                 ""
                                 (->> (range 12 256) (byte-array) (String.))
                                 "--SomeBoundary--"])
         :headers {"Accept-Ranges" "bytes"
                   "Content-Length" "418"
                   "Content-Type" "multipart/byteranges; boundary=SomeBoundary"}
         :status 206})))

(deftest wrap-range-header-multiple-ranges-needs-content-type-header
  (let [response {:status 200
                  :body "abcdefghijklmnop"}
        handler (wrap-range-header (constantly response))]
    (try (handler {:request-method :get
                   :headers {"Range" "bytes=3-5, 12-"}})
         (is false)
         (catch Exception _
           (is true)))))

(deftest wrap-range-header-suffix-range-overlaps-another-closed-range
  (let [response {:status 200
                  :body "12345"
                  :headers {"Content-Type" "font/woff"}}
        handler (wrap-range-header (constantly response))]
    (is (= (handler {:request-method :get
                     :headers {"Range" "bytes=2-3, -4"}})
           {:body nil
            :headers {"Accept-Ranges" "bytes"
                      "Content-Range" "bytes */5"}
            :status 416}))))

(deftest wrap-range-header-has-max-number-of-ranges
  (let [response {:status 200
                  :body (apply str (repeat 20 "a"))
                  :headers {"Content-Type" "application/xml"}}]
    (is (= (:status
             (run-range-middleware-and-convert-body-to-str
               response
               {:request-method :get
                :headers {"Range" "bytes=0-0,1-1,2-2,3-3,4-4,5-5,6-6,7-7,8-8,9-9"}}))
           206))
    (is (= (:status
             (run-range-middleware-and-convert-body-to-str
               response
               {:request-method :get
                :headers {"Range" "bytes=0-0,1-1,2-2,3-3,4-4,5-5,6-6,7-7,8-8,9-9,10-10"}}))
           416))))

(deftest wrap-range-header-has-max-buffer-size-for-content-length-unknown-bodies
  (let [stream-size (* 2 max-buffer-size-per-range-bytes)]
    (let [beyond-max-size-result (run-range-middleware-and-convert-body-to-str
                                   {:status 200
                                    :body (ByteArrayInputStream. (byte-array stream-size (byte \a)))}
                                   {:request-method :get
                                    :headers {"Range" (format "bytes=-%d" (inc max-buffer-size-per-range-bytes))}})]
      (is (= beyond-max-size-result
             {:body nil
              :headers {"Accept-Ranges" "bytes"
                        "Content-Range" (format "bytes */%d" stream-size)}
              :status 416})))

    (let [within-max-size-result (run-range-middleware-and-convert-body-to-str
                                   {:status 200
                                    :body (ByteArrayInputStream. (byte-array stream-size (byte \a)))}
                                   {:request-method :get
                                    :headers {"Range" (format "bytes=-%d" max-buffer-size-per-range-bytes)}}
                                   {}
                                   (ByteArrayOutputStream. max-buffer-size-per-range-bytes))]
      ;; separate out body because that comparing that inside a map takes a long time
      (is (= (dissoc within-max-size-result :body)
             {:headers {"Accept-Ranges" "bytes"
                        "Content-Length" (str max-buffer-size-per-range-bytes)
                        "Content-Range" (format "bytes %d-%d/%d"
                                                (- stream-size max-buffer-size-per-range-bytes)
                                                (dec stream-size)
                                                stream-size)}
              :status 206}))
      (is (= (-> within-max-size-result :body)
             (apply str (repeat max-buffer-size-per-range-bytes \a)))))))

(deftest wrap-range-header-has-unlimited-buffer-size-for-content-length-known-bodies
  (let [stream-size (* 2 max-buffer-size-per-range-bytes)
        requested-buffer-size (inc max-buffer-size-per-range-bytes)
        result (run-range-middleware-and-convert-body-to-str
                 {:status 200
                  :body (ByteArrayInputStream. (byte-array stream-size (byte \a)))
                  :headers {"Content-Length" (str stream-size)
                            "Content-Type" "text/plain"}}
                 {:request-method :get
                  :headers {"Range" (format "bytes=-%d" requested-buffer-size)}})]
    (is (= (dissoc result :body)
           {:headers {"Accept-Ranges" "bytes"
                      "Content-Range" (format "bytes %d-%d/%d"
                                              (- stream-size requested-buffer-size)
                                              (dec stream-size)
                                              stream-size)
                      "Content-Length" (str requested-buffer-size)
                      "Content-Type" "text/plain"}
            :status 206}))
    (is (= (-> result :body)
           (apply str (repeat requested-buffer-size \a))))))

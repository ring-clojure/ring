(ns ring.middleware.test.range
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [ring.middleware.range :refer :all]
            [ring.util.io :refer [string-input-stream]])
  (:import [java.io File]))

(def public-dir "test/ring/assets/")
(def binary-file-with-all-possible-bytes (File. ^String public-dir "binary-file-with-all-possible-bytes.bin"))

(deftest test-boundary-generator
  (let [boundary (*boundary-generator*)]
    (is (string? boundary))
    (is (not (empty? boundary)))
    (is (some? (re-find #"[a-zA-Z0-9]+" boundary))))

  (testing "is random"
    (is (not= (*boundary-generator*) (*boundary-generator*)))))

(deftest wrap-range-header-single-range-test
  (let [response {:status 200
                  :body "a very long response with a lot of bytes"}
        handler (wrap-range-header (constantly response))]
    (is (= (handler {:request-method :get
                     :headers {"Range" "bytes=2-10"}})
           {:body "very long"
            :headers {"Accept-Ranges" "bytes"
                      "Content-Length" "9"
                      "Content-Range" "bytes 2-10/40"}
            :status 206}))

    (testing "range must be valid"
      (is (= (handler {:request-method :get
                       :headers {"Range" "bytes=500-9001"}})
             (assoc-in response [:headers "Accept-Ranges"] "bytes")))
      (is (= (handler {:request-method :get
                       :headers {"Range" "bytes=a"}})
             (assoc-in response [:headers "Accept-Ranges"] "bytes")))
      (is (= (handler {:request-method :get
                       :headers {"Range" "bytes=0x1337"}})
             (assoc-in response [:headers "Accept-Ranges"] "bytes"))))

    (testing "response status must be 200"
      (let [handler-with-304 (wrap-range-header (constantly (assoc response :status 304)))]
        (is (= (handler-with-304 {:request-method :get
                                  :headers {"Range" "bytes=2-10"}})
               {:body "a very long response with a lot of bytes"
                :headers {"Accept-Ranges" "bytes"}
                :status 304}))))

    (testing "request method must be GET"
      (is (= (handler {:request-method :post
                       :headers {"Range" "bytes=2-10"}})
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
             (assoc-in response [:headers "Accept-Ranges"] "bytes")))
      (is (= (handler {:request-method :get
                       :headers {"Range" "bytes=47"}})
             (assoc-in response [:headers "Accept-Ranges"] "bytes")))
      (is (= (handler {:request-method :get
                       :headers {"Range" "bytes=-350"}})
             (assoc-in response [:headers "Accept-Ranges"] "bytes")))))

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
    (binding [*boundary-generator* (constantly "aboundary")]
      (let [ascii-response {:status 200
                            :body "a body with ASCII characters only"}
            ascii-handler (wrap-range-header (constantly ascii-response))]
        (is (= (ascii-handler {:request-method :get
                               :uri "some-file.txt"
                               :headers {"Range" "bytes=3-5,7-10"}})
               {:body (str/join "\r\n" ["--aboundary"
                                        "Content-Type: text/plain"
                                        "Content-Range: 3-5/33"
                                        ""
                                        "ody"
                                        "--aboundary"
                                        "Content-Type: text/plain"
                                        "Content-Range: 7-10/33"
                                        ""
                                        "with"
                                        "--aboundary--"])
                :headers {"Accept-Ranges" "bytes"
                          "Content-Length" "153"
                          "Content-Type" "multipart/byteranges; boundary=aboundary"}
                :status 206})))))

  (testing "file"
    (binding [*boundary-generator* (constantly "Mp4Boundary34")]
      (let [binary-response {:status 200
                             :body binary-file-with-all-possible-bytes}
            binary-handler (wrap-range-header (constantly binary-response))]
        (is (= (binary-handler {:request-method :get
                                :uri "some-video.mp4"
                                :headers {"Range" "bytes=7-20,  59-102,-13"}})
               {:body (str/join "\r\n" ["--Mp4Boundary34"
                                        "Content-Type: video/mp4"
                                        "Content-Range: 7-20/256"
                                        ""
                                        (->> (range 7 21) (byte-array) (String.))
                                        "--Mp4Boundary34"
                                        "Content-Type: video/mp4"
                                        "Content-Range: 59-102/256"
                                        ""
                                        (->> (range 59 103) (byte-array) (String.))
                                        "--Mp4Boundary34"
                                        "Content-Type: video/mp4"
                                        "Content-Range: 243-255/256"
                                        ""
                                        (->> (range 243 256) (byte-array) (String.))
                                        "--Mp4Boundary34--"])
                :headers {"Accept-Ranges" "bytes"
                          "Content-Length" "332"
                          "Content-Type" "multipart/byteranges; boundary=Mp4Boundary34"}
                :status 206})))))

  (testing "InputStream"
    (binding [*boundary-generator* (constantly "baaz")]
      (let [response {:status 200
                      :body (string-input-stream "another string example")}
            handler (wrap-range-header (constantly response))]
        (is (= (handler {:request-method :get
                         :uri "foo.mpeg"
                         :headers {"Range" "bytes=4-5,-6"}})
               {:body (str/join "\r\n" ["--baaz"
                                        "Content-Type: video/mpeg"
                                        "Content-Range: 4-5/22"
                                        ""
                                        "he"
                                        "--baaz"
                                        "Content-Type: video/mpeg"
                                        "Content-Range: 16-21/22"
                                        ""
                                        "xample"
                                        "--baaz--"])
                :headers {"Accept-Ranges" "bytes"
                          "Content-Length" "140"
                          "Content-Type" "multipart/byteranges; boundary=baaz"}
                :status 206})))))

  (testing "seq of strings"
    (binding [*boundary-generator* (constantly "somerandomstring")]
      (let [response {:status 200
                      :body (seq ["part 1\r\n" "string 2" "abc789"])}
            handler (wrap-range-header (constantly response))]
        (is (= (handler {:request-method :get
                         :uri "foo.png"
                         :headers {"Range" "bytes=0-0, 3-3, 6-7, -7"}})
               {:body (str/join "\r\n" ["--somerandomstring"
                                        "Content-Type: image/png"
                                        "Content-Range: 0-0/22"
                                        ""
                                        "p"
                                        "--somerandomstring"
                                        "Content-Type: image/png"
                                        "Content-Range: 3-3/22"
                                        ""
                                        "t"
                                        "--somerandomstring"
                                        "Content-Type: image/png"
                                        "Content-Range: 6-7/22"
                                        ""
                                        "\r\n"
                                        "--somerandomstring"
                                        "Content-Type: image/png"
                                        "Content-Range: 15-21/22"
                                        ""
                                        "2abc789"
                                        "--somerandomstring--"])
                :headers {"Accept-Ranges" "bytes"
                          "Content-Length" "321"
                          "Content-Type" "multipart/byteranges; boundary=somerandomstring"}
                :status 206}))))))

(deftest wrap-range-header-uses-existing-content-type-header-test
  (binding [*boundary-generator* (constantly "SomeBoundary")]
    (let [response {:status 200
                    :body binary-file-with-all-possible-bytes
                    :headers {"Content-Type" "image/jpeg"}}
          handler (wrap-range-header (constantly response))]
      (is (= (handler {:request-method :get
                       :uri "some-video.gif"
                       :headers {"Range" "bytes=3-5, 12-"}})
             {:body (str/join "\r\n" ["--SomeBoundary"
                                      "Content-Type: image/jpeg"
                                      "Content-Range: 3-5/256"
                                      ""
                                      (->> (range 3 6) (byte-array) (String.))
                                      "--SomeBoundary"
                                      "Content-Type: image/jpeg"
                                      "Content-Range: 12-255/256"
                                      ""
                                      (->> (range 12 256) (byte-array) (String.))
                                      "--SomeBoundary--"])
              :headers {"Accept-Ranges" "bytes"
                        "Content-Length" "662"
                        "Content-Type" "multipart/byteranges; boundary=SomeBoundary"}
              :status 206})))))

(deftest wrap-range-header-multiple-ranges-needs-content-type-header
  (let [response {:status 200
                  :body "abcdefghijklmnop"}
        handler (wrap-range-header (constantly response))]
    (try (handler {:request-method :get
                   :headers {"Range" "bytes=3-5, 12-"}})
         (is false)
         (catch Exception _
           (is true)))))
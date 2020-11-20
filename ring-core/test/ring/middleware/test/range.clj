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

(deftest test-parse-ranges
  (testing "single range"
    (is (= [{:start 0 :end 500}]
           (parse-ranges "bytes=0-499" 500)))
    (is (nil? (parse-ranges "bytes=0-499" 499)))
    (is (= [{:start 17 :end 35}]
           (parse-ranges "bytes=17-34" 35)))
    (is (nil? (parse-ranges "bytes=17-34" 16))))
  (testing "first-byte-pos"
    (is (= [{:start 1206 :end 1500}]
           (parse-ranges "bytes=1206-" 1500)))
    (is (nil? (parse-ranges "bytes=2934-" 2934)))
    (is (= [{:start 2934 :end 2935}]
           (parse-ranges "bytes=2934-" 2935))))
  (testing "suffix-byte-range-spec"
    (is (= [{:start 8988 :end 9000}]
           (parse-ranges "bytes=-12" 9000)))
    (is (= [{:start 4000 :end 9000}]
           (parse-ranges "bytes=-5000" 9000)))
    (is (= [{:start 0 :end 300}]
           (parse-ranges "bytes=-300" 300)))
    (is (nil? (parse-ranges "bytes=-300" 299))))
  (testing "multiple ranges"
    (is (= [{:start 0 :end 1}
            {:start 4 :end 5}]
           (parse-ranges "bytes=0-0,-1" 5)))
    (is (= [{:start 0 :end 1}
            {:start 2 :end 4}
            {:start 4 :end 5}]
           (parse-ranges "bytes=0-0,2-3,-1" 5)))
    (is (nil? (parse-ranges "bytes=0-0,2-3,-1" 4)))

    (is (= [{:start 5 :end 8}
            {:start 34 :end 301}
            {:start 400 :end 501}]
           (parse-ranges "bytes=5-7,34-300,400-500" 600)))
    (is (nil? (parse-ranges "bytes=5-7,34-300,400-500" 499)))

    (testing "overlapping ranges"
      (is (nil? (parse-ranges "bytes=100-,-400" 600)))
      (is (= [{:start 100 :end 200}
              {:start 200 :end 600}]
             (parse-ranges "bytes=100-199,-400" 600)))
      (is (nil? (parse-ranges "bytes=100-199,-401" 600))))

    (testing "space between ranges"
      (is (= [{:start 0 :end 1}
              {:start 4 :end 5}]
             (parse-ranges "bytes=0-0, -1" 5)))
      (is (= [{:start 0 :end 14}
              {:start 600 :end 3211}
              {:start 8988 :end 9000}]
             (parse-ranges "bytes=0-13,  600-3210,-12" 9000)))))
  (testing "garbage input"
    (is (nil? (parse-ranges "foo" 100)))
    (is (nil? (parse-ranges "bar=1-10" 100)))
    (is (nil? (parse-ranges "bytes1-10" 100)))
    (is (nil? (parse-ranges "bytes=3" 100)))))

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
(ns ring.middleware.test.file-info
  (:require [clojure.test :refer :all]
            [ring.middleware.file-info :refer :all])
  (:import [java.io File]))

(def non-file-app (wrap-file-info (constantly {:headers {} :body "body"})))

(def known-file (File. "test/ring/assets/plain.txt"))
(def known-file-app (wrap-file-info (constantly {:headers {} :body known-file})))

(def unknown-file (File. "test/ring/assets/random.xyz"))
(def unknown-file-app (wrap-file-info (constantly {:headers {} :body unknown-file})))

(defmacro with-last-modified 
  "Lets us use a known file modification time for tests, without permanently changing
   the file's modification time."
  [[file new-time] form]
  `(let [old-time# (.lastModified ~file)] 
     (.setLastModified ~file ~(* new-time 1000))
     (try ~form
          (finally (.setLastModified ~file old-time#)))))

(def custom-type-app
  (wrap-file-info
    (constantly {:headers {} :body known-file})
    {"txt" "custom/type"}))

(deftest wrap-file-info-non-file-response
  (is (= {:headers {} :body "body"} (non-file-app {}))))

(deftest wrap-file-info-known-file-response
  (with-last-modified [known-file 1263506400]
    (is (= {:headers {"Content-Type"   "text/plain"
                      "Content-Length" "6"
                      "Last-Modified"  "Thu, 14 Jan 2010 22:00:00 +0000"}
            :body    known-file}
           (known-file-app {:headers {}})))))

(deftest wrap-file-info-unknown-file-response
  (with-last-modified [unknown-file 1263506400]
    (is (= {:headers {"Content-Type"   "application/octet-stream"
                      "Content-Length" "7"
                      "Last-Modified"  "Thu, 14 Jan 2010 22:00:00 +0000"}
            :body    unknown-file}
           (unknown-file-app {:headers {}})))))

(deftest wrap-file-info-custom-mime-types
  (with-last-modified [known-file 0]
    (is (= {:headers {"Content-Type"   "custom/type"
                      "Content-Length" "6"
                      "Last-Modified"  "Thu, 01 Jan 1970 00:00:00 +0000"}
            :body known-file}
           (custom-type-app {:headers {}})))))

(deftest wrap-file-info-if-modified-since-hit
  (with-last-modified [known-file 1263506400]
    (is (= {:status  304
            :headers {"Content-Type"   "text/plain"
                      "Content-Length" "0"
                      "Last-Modified"  "Thu, 14 Jan 2010 22:00:00 +0000"}
            :body    ""}
           (known-file-app
             {:headers {"if-modified-since" "Thu, 14 Jan 2010 22:00:00 +0000" }})))))

(deftest wrap-file-info-if-modified-miss
  (with-last-modified [known-file 1263506400]
    (is (= {:headers {"Content-Type" "text/plain"
                      "Content-Length" "6"
                      "Last-Modified" "Thu, 14 Jan 2010 22:00:00 +0000"}
            :body    known-file}
           (known-file-app
             {:headers {"if-modified-since" "Wed, 13 Jan 2010 22:00:00 +0000"}})))))

(deftest wrap-file-info-cps-test
  (testing "file response"
    (with-last-modified [known-file 1263506400]
      (let [handler   (wrap-file-info
                       (fn [_ respond _] (respond {:headers {} :body known-file})))
            response  (promise)
            exception (promise)]
        (handler {:headers {}} response exception)
        (is (= {:headers {"Content-Type"   "text/plain"
                          "Content-Length" "6"
                          "Last-Modified"  "Thu, 14 Jan 2010 22:00:00 +0000"}
                :body    known-file}
               @response))
        (is (not (realized? exception))))))
  
  (testing "non-file response"
    (let [handler   (wrap-file-info
                     (fn [_ respond _] (respond {:headers {} :body "body"})))
          response  (promise)
          exception (promise)]
      (handler {:headers {}} response exception)
      (is (= {:headers {} :body "body"}
             @response))
      (is (not (realized? exception))))))

(deftest file-info-response-test
  (is (fn? file-info-response)))

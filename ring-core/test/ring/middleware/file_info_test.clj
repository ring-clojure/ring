(ns ring.middleware.file-info-test
  (:use clojure.test
        ring.middleware.file-info)
  (:import java.io.File))

(def non-file-app (wrap-file-info (constantly {:headers {} :body "body"})))

(def known-file (File. "test/ring/assets/plain.txt"))
(def known-file-app (wrap-file-info (constantly {:headers {} :body known-file})))

(def unknown-file (File. "test/ring/assets/random.xyz"))
(def unknown-file-app (wrap-file-info (constantly {:headers {} :body unknown-file})))

(defmacro with-custom-last-modified [file new-time form]
  "Lets us use a known file modification time for tests, without permanently changing
   the file's modification time"
  `(let [old-time# (.lastModified ~file)]
        (do
          (.setLastModified ~file (* 1000 ~new-time));use seconds, not millis
          (let [result# ~form]
            (do
              (.setLastModified ~file old-time#)
              result#)))))

(def custom-type-app
  (wrap-file-info
    (constantly {:headers {} :body known-file})
    {"txt" "custom/type"}))

(deftest wrap-file-info-non-file-response
  (is (= {:headers {} :body "body"} (non-file-app {}))))

(deftest wrap-file-info-known-file-response
  (with-custom-last-modified known-file 1263506400
    (is (= {:headers {"Content-Type" "text/plain" "Content-Length" "6"
                      "Last-Modified" "Thu, 14 Jan 2010 22:00:00 +0000"}
            :body    known-file}
           (known-file-app {})))))

(deftest wrap-file-info-unknown-file-response
  (is (= {:headers {"Content-Type" "application/octet-stream" "Content-Length" "7"}
          :body    unknown-file}
         (unknown-file-app {}))))

(deftest wrap-file-info-custom-mime-types
  (with-custom-last-modified known-file 0
    (is (= {:headers {"Content-Type" "custom/type" "Content-Length" "6"
                      "Last-Modified" "Thu, 01 Jan 1970 00:00:00 +0000"}
            :body known-file}
           (custom-type-app {})))))

(deftest wrap-file-info-if-modified-since-hit
  (with-custom-last-modified known-file 1263506400
    (is (= {:status  304
            :headers {"Content-Type" "text/plain" "Content-Length" "0"
                      "Last-Modified" "Thu, 14 Jan 2010 22:00:00 +0000"}
            :body    ""}
           (known-file-app {:headers {"if-modified-since" "Thu, 14 Jan 2010 22:00:00 +0000" }})))))

(deftest wrap-file-info-if-modified-miss
  (with-custom-last-modified known-file 1263506400
    (is (= {:headers {"Content-Type" "text/plain" "Content-Length" "6"
                      "Last-Modified" "Thu, 14 Jan 2010 22:00:00 +0000"}
            :body    known-file}
           (known-file-app {:headers {"if-modified-since" "Wed, 13 Jan 2010 22:00:00 +0000"}})))))

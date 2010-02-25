(ns ring.util.request-test
  (:use clojure.test
        ring.util.request))

(deftest test-ssl?
  (is (ssl? {:scheme :https}))
  (is (not (ssl? {:scheme :http}))))

(deftest test-server-host
  (is (= "google.com"
         (server-host {:server-name "ask.com" :server-port 80
                       :headers {"x-forwarded-host" "google.com"
                                 "host"             "yahoo.com"}})))
  (is (= "yahoo.com"
         (server-host {:server-name "ask.com" :server-port 80
                       :headers {"host" "yahoo.com"}})))
  (is (= "ask.com"
         (server-host {:server-name "ask.com" :server-port 80}))))

(deftest test-full-uri
  (is (= "https://google.com/foo/bar"
         (full-uri {:uri "/foo/bar" :scheme :https
                    :headers {"host" "google.com"}}))))

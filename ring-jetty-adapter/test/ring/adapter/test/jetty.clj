(ns ring.adapter.test.jetty
  (:use clojure.test
        ring.adapter.jetty)
  (:require [clj-http.client :as http]))

(defn- hello-world [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defmacro with-server [options & body]
  `(let [server# (run-jetty hello-world ~(assoc options :join? false))]
     (try
       ~@body
       (finally (.stop server#)))))

(deftest test-run-jetty
  (testing "HTTP server"
    (with-server {:port 4347}
      (let [response (http/get "http://localhost:4347")]
        (is (= (:status response) 200))
        (is (.startsWith (get-in response [:headers "content-type"])
                         "text/plain"))
        (is (= (:body response) "Hello World")))))

  (testing "HTTPS server"
    (with-server {:port 4347
                  :ssl-port 4348
                  :keystore "test/keystore.jks"
                  :key-password "password"}
      (let [response (http/get "https://localhost:4348" {:insecure? true})]
        (is (= (:status response) 200))
        (is (= (:body response) "Hello World"))))))

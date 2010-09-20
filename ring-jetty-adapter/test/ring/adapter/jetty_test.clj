(ns ring.adapter.jetty-test
  (:use clojure.test
        ring.adapter.jetty)
  (:require [clj-http.client :as http]))

(defn- hello-world [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(deftest jetty-test
  (let [server (run-jetty hello-world {:port 4347, :join? false})]
    (try
      (Thread/sleep 2000)
      (let [response (http/get "http://localhost:4347")]
        (is (= (:status response) 200))
        (is (.startsWith (get-in response [:headers "content-type"])
                         "text/plain"))
        (is (= (:body response) "Hello World\n")))
      (finally (.stop server)))))

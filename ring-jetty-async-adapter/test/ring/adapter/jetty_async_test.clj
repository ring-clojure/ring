(ns ring.adapter.jetty-async-test
  (:use clojure.test
        ring.adapter.jetty-async)
  (:require [clj-http.client :as http]))

(defn- hello-world [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(deftest jetty-async-test
  (let [server (run-jetty-async hello-world {:port 4347, :join? false})]
    (try
      (Thread/sleep 2000)
      (let [response (http/get "http://localhost:4347")]
        (is (= (:status response) 200))
        (is (.startsWith (get-in response [:headers "content-type"])
                         "text/plain"))
        (is (= (:body response) "Hello World\n")))
      (finally (.stop server)))))

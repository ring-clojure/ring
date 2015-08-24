(ns ring.adapter.test.jetty
  (:require [clojure.test :refer :all]
            [ring.adapter.jetty :refer :all]
            [clj-http.client :as http])
  (:import [org.eclipse.jetty.util.thread QueuedThreadPool]
           [org.eclipse.jetty.server Server Request]
           [org.eclipse.jetty.server.handler AbstractHandler]
           [java.net ServerSocket ConnectException]))

(defn- hello-world [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defn- content-type-handler [content-type]
  (constantly
   {:status  200
    :headers {"Content-Type" content-type}
    :body    ""}))

(defn- echo-handler [request]
  {:status 200
   :headers {"request-map" (str (dissoc request :body))}
   :body (:body request)})

(defn- all-threads []
  (.keySet (Thread/getAllStackTraces)))

(defmacro with-server [app options & body]
  `(let [server# (run-jetty ~app ~(assoc options :join? false))]
     (try
       ~@body
       (finally (.stop server#)))))

(defn- find-free-local-port []
  (let [socket (ServerSocket. 0)]
    (let [port (.getLocalPort socket)]
      (.close socket)
      port)))

(def test-port (find-free-local-port))

(def test-ssl-port (find-free-local-port))

(def test-url (str "http://localhost:" test-port))

(def test-ssl-url (str "https://localhost:" test-ssl-port))

(deftest test-run-jetty
  (testing "HTTP server"
    (with-server hello-world {:port test-port}
      (let [response (http/get test-url)]
        (is (= (:status response) 200))
        (is (.startsWith (get-in response [:headers "content-type"])
                         "text/plain"))
        (is (= (:body response) "Hello World")))))

  (testing "HTTPS server"
    (with-server hello-world {:port test-port
                              :ssl-port test-ssl-port
                              :keystore "test/keystore.jks"
                              :key-password "password"}
      (let [response (http/get test-ssl-url {:insecure? true})]
        (is (= (:status response) 200))
        (is (= (:body response) "Hello World")))))

  (testing "HTTPS-only server"
    (with-server hello-world {:http? false
                              :port test-port
                              :ssl-port test-ssl-port
                              :keystore "test/keystore.jks"
                              :key-password "password"}
      (let [response (http/get test-ssl-url {:insecure? true})]
        (is (= (:status response) 200))
        (is (= (:body response) "Hello World")))
      (is (thrown-with-msg? ConnectException #"Connection refused"
                            (http/get test-url)))))

  (testing "configurator set to run last"
    (let [max-threads 20
          new-handler  (proxy [AbstractHandler] []
                         (handle [_ ^Request base-request request response]))
          configurator (fn [server]
                         (.setMaxThreads (.getThreadPool server) max-threads)
                         (.setHandler server new-handler))
          server (run-jetty hello-world
                            {:join? false :port test-port :configurator configurator})]
      (is (= (.getMaxThreads (.getThreadPool server)) max-threads))
      (is (identical? new-handler (.getHandler server)))
      (is (= 1 (count (.getHandlers server))))
      (.stop server)))

  (testing "setting daemon threads"
    (testing "default (daemon off)"
      (let [server (run-jetty hello-world {:port test-port :join? false})]
        (is (not (.. server getThreadPool isDaemon)))
        (.stop server)))
    (testing "daemon on"
      (let [server (run-jetty hello-world {:port test-port :join? false :daemon? true})]
        (is (.. server getThreadPool isDaemon))
        (.stop server)))
    (testing "daemon off"
      (let [server (run-jetty hello-world {:port test-port :join? false :daemon? false})]
        (is (not (.. server getThreadPool isDaemon)))
        (.stop server))))

  (testing "setting max idle timeout"
    (let [server (run-jetty hello-world {:port test-port
                                         :ssl-port test-ssl-port
                                         :keystore "test/keystore.jks"
                                         :key-password "password"
                                         :join? false
                                         :max-idle-time 5000})
          connectors (. server getConnectors)]
      (is (= 5000 (. (first connectors) getIdleTimeout)))
      (is (= 5000 (. (second connectors) getIdleTimeout)))
      (.stop server)))

  (testing "using the default max idle time"
    (let [server (run-jetty hello-world {:port test-port
                                         :ssl-port test-ssl-port
                                         :keystore "test/keystore.jks"
                                         :key-password "password"
                                         :join? false})
          connectors (. server getConnectors)]
      (is (= 200000 (. (first connectors) getIdleTimeout)))
      (is (= 200000 (. (second connectors) getIdleTimeout)))
      (.stop server)))

  (testing "setting min-threads"
    (let [server (run-jetty hello-world {:port test-port
                                         :min-threads 3
                                         :join? false})
          thread-pool (. server getThreadPool)]
      (is (= 3 (. thread-pool getMinThreads)))
      (.stop server)))

  (testing "default min-threads"
    (let [server (run-jetty hello-world {:port test-port
                                         :join? false})
          thread-pool (. server getThreadPool)]
      (is (= 8 (. thread-pool getMinThreads)))
      (.stop server)))

  (testing "default character encoding"
    (with-server (content-type-handler "text/plain") {:port test-port}
      (let [response (http/get test-url)]
        (is (.contains
             (get-in response [:headers "content-type"])
             "text/plain")))))

  (testing "custom content-type"
    (with-server (content-type-handler "text/plain;charset=UTF-16;version=1") {:port test-port}
      (let [response (http/get test-url)]
        (is (= (get-in response [:headers "content-type"])
               "text/plain;charset=UTF-16;version=1")))))

  (testing "request translation"
    (with-server echo-handler {:port test-port}
      (let [response (http/post (str test-url "/foo/bar/baz?surname=jones&age=123") {:body "hello"})]
        (is (= (:status response) 200))
        (is (= (:body response) "hello"))
        (let [request-map (read-string (get-in response [:headers "request-map"]))]
          (is (= (:query-string request-map) "surname=jones&age=123"))
          (is (= (:uri request-map) "/foo/bar/baz"))
          (is (= (:content-length request-map) 5))
          (is (= (:character-encoding request-map) "UTF-8"))
          (is (= (:request-method request-map) :post))
          (is (= (:content-type request-map) "text/plain; charset=UTF-8"))
          (is (= (:remote-addr request-map) "127.0.0.1"))
          (is (= (:scheme request-map) :http))
          (is (= (:server-name request-map) "localhost"))
          (is (= (:server-port request-map) test-port))
          (is (= (:ssl-client-cert request-map) nil))))))

  ;; Unable to get test working with Jetty 9
  (comment
    (testing "resource cleanup on exception"
      (with-server hello-world {:port test-port}
        (let [thread-count (count (all-threads))]
          (is (thrown? Exception (run-jetty hello-world {:port test-port})))
          (loop [i 0]
            (when (and (< i 400) (not= thread-count (count (all-threads))))
              (Thread/sleep 250)
              (recur (inc i))))
          (is (= thread-count (count (all-threads)))))))))

(ns ring.adapter.test.jetty
  (:require [clojure.test :refer :all]
            [ring.adapter.jetty :refer :all]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [less.awful.ssl :as less-ssl]
            [ring.core.protocols :as p])
  (:import [org.eclipse.jetty.util.thread QueuedThreadPool]
           [org.eclipse.jetty.util BlockingArrayQueue]
           [org.eclipse.jetty.server Server Request SslConnectionFactory]
           [org.eclipse.jetty.server.handler AbstractHandler]
           [java.net ServerSocket ConnectException]
           [java.security KeyStore]
           [java.io SequenceInputStream ByteArrayInputStream InputStream
                    IOException]
           [org.apache.http MalformedChunkCodingException]))

(defn- hello-world [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defn client-cert-handler [request]
  (if (nil? (:ssl-client-cert request))
    {:status 403}
    {:status 200}))

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

(defn ssl-context []
  (less-ssl/ssl-context "test/server.key"
                        "test/server.crt"
                        "test/server.crt"))

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

(defn- get-ssl-context-factory
  [^Server s]
  (->> (seq (.getConnectors s))
       (mapcat #(seq (.getConnectionFactories %)))
       (filter #(instance? SslConnectionFactory %))
       (first)
       (.getSslContextFactory)))

(defn- exclude-ciphers [server]
  (set (.getExcludeCipherSuites (get-ssl-context-factory server))))

(defn- exclude-protocols [server]
  (set (.getExcludeProtocols (get-ssl-context-factory server))))

(def test-port (find-free-local-port))

(def test-ssl-port (find-free-local-port))

(def test-url (str "http://localhost:" test-port))

(def test-ssl-url (str "https://localhost:" test-ssl-port))

(def nil-keystore
  (doto (KeyStore/getInstance (KeyStore/getDefaultType)) (.load nil)))

(def test-ssl-options
  {:port         test-port
   :ssl?         true
   :ssl-port     test-ssl-port
   :keystore     nil-keystore
   :key-password "hunter2"
   :join?        false})

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

  (testing "HTTPS server with jceks keystore"
    (with-server hello-world {:port test-port
                              :ssl-port test-ssl-port
                              :keystore "test/keystore.jceks"
                              :keystore-type "jceks"
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

  (testing "HTTPS server that needs client certs"
    (with-server client-cert-handler {:client-auth :need
                                      :keystore "test/keystore.jks"
                                      :key-password "password"
                                      :port test-port
                                      :ssl? true
                                      :ssl-port test-ssl-port}
      (is (thrown? java.io.IOException
                   (http/get test-ssl-url {:insecure? true}))
          "missing client certs will cause an exception")
      (let [response (http/get test-ssl-url {:insecure? true
                                             :keystore "test/keystore.jks"
                                             :keystore-pass "password"
                                             :trust-store "test/keystore.jks"
                                             :trust-store-pass "password"})]
        (is (= 200 (:status response))
            "sending client certs will receive 200 from handler"))))

  (testing "HTTPS server that wants client certs"
    (with-server client-cert-handler {:client-auth :want
                                      :keystore "test/keystore.jks"
                                      :key-password "password"
                                      :port test-port
                                      :ssl? true
                                      :ssl-port test-ssl-port}
      (let [response (http/get test-ssl-url {:insecure? true
                                             :throw-exceptions false})]
        (is (= 403 (:status response))
            "missing client certs will result in 403 from handler"))
      (let [response (http/get test-ssl-url {:insecure? true
                                             :keystore "test/keystore.jks"
                                             :keystore-pass "password"
                                             :trust-store "test/keystore.jks"
                                             :trust-store-pass "password"})]
        (is (= 200 (:status response))
            "sending client certs will receive 200 from handler"))))

  (testing "HTTPS server using :ssl-context"
    (with-server hello-world {:port test-port
                              :ssl-port test-ssl-port
                              :ssl-context (ssl-context)}
      (let [response (http/get test-ssl-url {:insecure? true})]
        (is (= (:status response) 200))
        (is (= (:body response) "Hello World")))))

  (testing "HTTPS server using :ssl-context that needs client certs"
    (with-server client-cert-handler {:client-auth :need
                                      :ssl-context (ssl-context)
                                      :port test-port
                                      :ssl? true
                                      :ssl-port test-ssl-port}
      (is (thrown? java.io.IOException
                   (http/get test-ssl-url {:insecure? true}))
          "missing client certs will cause an exception")
      (let [response (http/get test-ssl-url {:insecure? true
                                             :keystore "test/keystore.jks"
                                             :keystore-pass "password"
                                             :trust-store "test/keystore.jks"
                                             :trust-store-pass "password"})]
        (is (= 200 (:status response))
            "sending client certs will receive 200 from handler"))))

  (testing "HTTPS server using :ssl-context that wants client certs"
    (with-server client-cert-handler {:client-auth :want
                                      :ssl-context (ssl-context)
                                      :port test-port
                                      :ssl? true
                                      :ssl-port test-ssl-port}
      (let [response (http/get test-ssl-url {:insecure? true
                                             :throw-exceptions false})]
        (is (= 403 (:status response))
            "missing client certs will result in 403 from handler"))
      (let [response (http/get test-ssl-url {:insecure? true
                                             :keystore "test/keystore.jks"
                                             :keystore-pass "password"
                                             :trust-store "test/keystore.jks"
                                             :trust-store-pass "password"})]
        (is (= 200 (:status response))
            "sending client certs will receive 200 from handler"))))

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

  (testing "default thread-idle-timeout"
    (let [server (run-jetty hello-world {:port test-port
                                         :join? false})
          thread-pool (. server getThreadPool)]
      (is (= 60000 (. thread-pool getIdleTimeout)))
      (.stop server)))

  (testing "setting thread-idle-timeout"
    (let [server (run-jetty hello-world {:port test-port
                                         :join? false
                                         :thread-idle-timeout 1000})
          thread-pool (. server getThreadPool)]
      (is (= 1000 (. thread-pool getIdleTimeout)))
      (.stop server)))

  (testing "providing custom thread-pool"
    (let [pool   (QueuedThreadPool.)
          server (run-jetty hello-world {:port test-port
                                         :join? false
                                         :thread-pool pool})]
      (is (identical? pool (.getThreadPool server)))
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

  (testing "sending 'Server' header in HTTP response'"
    (testing ":send-server-version? set to default value (true)"
      (with-server hello-world {:port test-port}
        (let [response (http/get test-url)]
          (is (contains? (:headers response) "Server")))))
    (testing ":send-server-version? set to true"
      (with-server hello-world {:port test-port
                                :send-server-version? true}
        (let [response (http/get test-url)]
          (is (contains? (:headers response) "Server")))))
    (testing ":send-server-version? set to false"
      (with-server hello-world {:port test-port
                                :send-server-version? false}
        (let [response (http/get test-url)]
          (is (not (contains? (:headers response) "Server")))))))
  (testing "excluding cipher suites"
    (let [cipher  "SSL_RSA_WITH_NULL_MD5"
          options (assoc test-ssl-options :exclude-ciphers [cipher])
          server  (run-jetty echo-handler options)]
      (try
        (is (contains? (exclude-ciphers server) cipher))
        ;; The operation is additive; it doesn't replace ciphers that Jetty
        ;; excludes by default
        (is (> (count (exclude-ciphers server)) 1))
        (finally
          (.stop server)))))

  (testing "replacing excluded cipher suites"
    (let [cipher   "SSL_RSA_WITH_NULL_MD5"
          options  (assoc test-ssl-options
                          :exclude-ciphers [cipher]
                          :replace-exclude-ciphers? true)
          server   (run-jetty echo-handler options)
          excludes (exclude-ciphers server)]
      (try
        (is (= (first excludes) cipher))
        (is (= (seq (rest excludes)) nil))
        (finally
          (.stop server)))))

  (testing "excluding cipher protocols"
    (let [protocol "SSLv2Hello"
          options  (assoc test-ssl-options :exclude-protocols [protocol])
          server   (run-jetty echo-handler options)]
      (try
        (is (contains? (exclude-protocols server) protocol))
        ;; The operation is additive; it doesn't replace protocols that Jetty
        ;; excludes by default
        (is (> (count (exclude-protocols server)) 1))
        (finally
          (.stop server)))))

  (testing "replacing excluded cipher protocols"
    (let [protocol "SSLv2Hello"
          options  (assoc test-ssl-options
                          :exclude-protocols [protocol]
                          :replace-exclude-protocols? true)
          server   (run-jetty echo-handler options)
          excludes (exclude-protocols server)]
      (try
        (is (= (first excludes) protocol))
        (is (= (seq (rest excludes)) nil))
        (finally
          (.stop server)))))

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

(defn- chunked-stream-with-error
  ([request]
   {:status  200
    :headers {"Transfer-Encoding" "chunked"}
    :body    (SequenceInputStream.
               (ByteArrayInputStream. (.getBytes (str (range 100000)) "UTF-8"))
               (proxy [InputStream] []
                 (read
                   ([] (throw (IOException. "test error")))
                   ([^bytes _] (throw (IOException. "test error")))
                   ([^bytes _ _ _] (throw (IOException. "test error"))))))})
  ([request response raise]
   (response (chunked-stream-with-error request))))

(defn- chunked-lazy-seq-with-error
  ([request]
   {:status  200
    :headers {"Transfer-Encoding" "chunked"}
    :body    (lazy-cat (range 100000)
                       (throw (IOException. "test error")))})
  ([request response raise]
   (response (chunked-lazy-seq-with-error request))))

(deftest streaming-with-error
  (testing "chunked stream without sending termination chunk on error"
    (with-server chunked-stream-with-error {:port test-port}
      (is (thrown? MalformedChunkCodingException (http/get test-url)))))

  (testing "chunked sequence without sending termination chunk on error"
    (with-server chunked-lazy-seq-with-error {:port test-port}
      (is (thrown? MalformedChunkCodingException (http/get test-url)))))

  (testing "async chunked stream without sending termination chunk on error"
    (with-server chunked-stream-with-error {:port test-port :async? true}
      (is (thrown? MalformedChunkCodingException (http/get test-url)))))

  (testing "async chunked sequence without sending termination chunk on error"
    (with-server chunked-lazy-seq-with-error {:port test-port :async? true}
      (is (thrown? MalformedChunkCodingException (http/get test-url))))))

(def thread-exceptions (atom []))

(defn- hello-world-cps [request respond raise]
  (respond {:status  200
            :headers {"Content-Type" "text/plain"}
            :body    "Hello World"}))

(defn- hello-world-cps-future [request respond raise]
  (future
    (try (respond {:status  200
                   :headers {"Content-Type" "text/plain"}
                   :body    "Hello World"})
         (catch Exception ex
           (swap! thread-exceptions conj ex)))))

(defn- hello-world-streaming [request respond raise]
  (future
    (respond
     {:status  200
      :headers {"Content-Type" "text/event-stream"}
      :body    (reify p/StreamableResponseBody
                 (write-body-to-stream [_ _ output]
                   (future
                     (with-open [w (io/writer output)]
                       (Thread/sleep 100)
                       (.write w "data: hello\n\n")
                       (.flush w)
                       (Thread/sleep 100)
                       (.write w "data: world\n\n")
                       (.flush w)))))})))

(defn- hello-world-streaming-long [request respond raise]
  (respond
   {:status  200
    :headers {"Content-Type" "text/event-stream"}
    :body    (reify p/StreamableResponseBody
               (write-body-to-stream [_ _ output]
                 (future
                   (with-open [w (io/writer output)]
                     (dotimes [i 10]
                       (Thread/sleep 100)
                       (.write w (str "data: " i "\n\n"))
                       (.flush w))))))}))

(defn- error-cps [request respond raise]
  (raise (ex-info "test" {:foo "bar"})))

(defn- sometimes-error-cps [request respond raise]
  (if (= (:uri request) "/error")
    (error-cps request respond raise)
    (hello-world-cps request respond raise)))

(defn- hello-world-slow-cps [request respond raise]
  (future (Thread/sleep 1000)
          (respond {:status  200
                    :headers {"Content-Type" "text/plain"}
                    :body    "Hello World"})))

(deftest run-jetty-cps-test
  (testing "async response in future"
    (reset! thread-exceptions [])
    (with-server hello-world-cps-future {:port test-port, :async? true}
      (let [response (http/get test-url)]
        (Thread/sleep 100)
        (is (empty? @thread-exceptions))
        (is (= (:status response) 200))
        (is (.startsWith (get-in response [:headers "content-type"])
                         "text/plain"))
        (is (= (:body response) "Hello World")))))

  (testing "async response"
    (with-server hello-world-cps {:port test-port, :async? true}
      (let [response (http/get test-url)]
        (is (= (:status response) 200))
        (is (.startsWith (get-in response [:headers "content-type"])
                         "text/plain"))
        (is (= (:body response) "Hello World")))))

  (testing "streaming response"
    (with-server hello-world-streaming {:port test-port, :async? true}
      (let [response (http/get test-url)]
        (is (= (:status response) 200))
        (is (.startsWith (get-in response [:headers "content-type"])
                         "text/event-stream"))
        (is (= (:body response)
               "data: hello\n\ndata: world\n\n")))))

  (testing "error response"
    (with-server error-cps {:port test-port, :async? true}
      (let [response (http/get test-url {:throw-exceptions false})]
        (is (= (:status response) 500)))))

  (testing "mixed error with normal responses"
    (with-server sometimes-error-cps {:port test-port, :async? true}
      (let [response (http/get (str test-url "/error") {:throw-exceptions false})]
        (is (= (:status response) 500)))
      (let [response (http/get test-url {:throw-exceptions false})]
        (is (= (:status response) 200)))
      (let [response (http/get (str test-url "/error") {:throw-exceptions false})]
        (is (= (:status response) 500)))
      (let [response (http/get test-url {:throw-exceptions false})]
        (is (= (:status response) 200)))))

  (testing "async context default"
    (with-server hello-world-streaming-long {:port test-port, :async? true}
      (let [response (http/get test-url)]
        (is (= (:body response)
               (apply str (for [i (range 10)] (str "data: " i "\n\n"))))))))

  (testing "async timeout handler"
    (testing "when no timeout handler is passed, behaviour is unchanged"
      (with-server hello-world-slow-cps {:port test-port
                                         :async? true
                                         :async-timeout 250}
        (let [response (http/get test-url {:throw-exceptions false})]
          (is (= (:status response)
                 500)))))

    (testing "with timeout handlers, ring-style responses are generated"
      (with-server hello-world-slow-cps
        {:port test-port
         :async? true
         :async-timeout 200
         :async-timeout-handler (fn [request respond raise]
                                  (respond
                                   {:status 503
                                    :headers {"Content-Type" "text/plain"}
                                    :body "Request timed out"}))}
        (let [response (http/get test-url {:throw-exceptions false})]
          (is (= (:body response)
                 "Request timed out"))
          (is (= (:status response)
                 503))))

      (with-server hello-world-slow-cps
        {:port test-port
         :async? true
         :async-timeout 200
         :async-timeout-handler (fn [request respond raise]
                                  (raise
                                   (ex-info "An exception was thrown" {})))}
        (let [response (http/get (str test-url "/test-path/testing")
                                 {:throw-exceptions false})]
          (is (.contains ^String (:body response)
                         "An exception was thrown"))
          (is (= (:status response)
                 500)))))))

(def call-count (atom 0))

(defn- broken-handler [request]
  (swap! call-count inc)
  (throw (ex-info "unhandled exception" {})))

(defn- broken-handler-cps [request respond raise]
  (swap! call-count inc)
  (raise (ex-info "unhandled exception" {})))

(testing "broken handler is only called once"
  (reset! call-count 0)
  (with-server broken-handler {:port test-port}
    (try (http/get test-url)
      (catch Exception _ nil))
    (is (= 1 @call-count))))

(testing "broken async handler is only called once"
  (reset! call-count 0)
  (with-server broken-handler-cps {:async? true :port test-port}
    (try (http/get test-url)
      (catch Exception _ nil))
    (is (= 1 @call-count))))

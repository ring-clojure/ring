(ns ring.adapter.jetty
  "A Ring adapter that uses the Jetty 9 embedded web server.

  Adapters are used to convert Ring handlers into running web servers."
  (:require [ring.util.servlet :as servlet])
  (:import [org.eclipse.jetty.server
            Request
            Server
            ServerConnector
            ConnectionFactory
            HttpConfiguration
            HttpConnectionFactory
            SslConnectionFactory
            SecureRequestCustomizer]
           [org.eclipse.jetty.server.handler AbstractHandler]
           [org.eclipse.jetty.util.thread ThreadPool QueuedThreadPool]
           [org.eclipse.jetty.util.ssl SslContextFactory]
           [javax.servlet.http HttpServletRequest HttpServletResponse]))

(defmulti handle-response-map
  (fn [^Request base-request ^HttpServletRequest request ^HttpServletResponse response response-map]
    (:async response-map)))

(defmethod handle-response-map nil
  [^Request base-request ^HttpServletRequest request ^HttpServletResponse response response-map]
  (servlet/update-servlet-response response response-map)
  (.setHandled base-request true))

(defmethod handle-response-map :ring
  [^Request base-request ^HttpServletRequest request ^HttpServletResponse response response-map]
  (let [reactor (:reactor response-map)
        ac (.startAsync request)
        emit (fn [response-map]
               (servlet/update-servlet-response (.getResponse ac) response-map)
               (.complete ac))]
    (reactor emit)))

(defn- ^AbstractHandler proxy-handler [handler]
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request response]
      (let [request-map  (servlet/build-request-map request)
            response-map (handler request-map)]
        (when response-map
          (handle-response-map base-request request response response-map))))))

(defn- ^ServerConnector server-connector [server & factories]
  (ServerConnector. server (into-array ConnectionFactory factories)))

(defn- ^HttpConfiguration http-config [options]
  (doto (HttpConfiguration.)
    (.setSendDateHeader (:send-date-header? options true))
    (.setOutputBufferSize (:output-buffer-size options 32768))
    (.setRequestHeaderSize (:request-header-size options 8192))
    (.setResponseHeaderSize (:response-header-size options 8192))))

(defn- ^ServerConnector http-connector [server options]
  (let [http-factory (HttpConnectionFactory. (http-config options))]
    (doto (server-connector server http-factory)
      (.setPort (options :port 80))
      (.setHost (options :host))
      (.setIdleTimeout (options :max-idle-time 200000)))))

(defn- ^SslContextFactory ssl-context-factory [options]
  (let [context (SslContextFactory.)]
    (if (string? (options :keystore))
      (.setKeyStorePath context (options :keystore))
      (.setKeyStore context ^java.security.KeyStore (options :keystore)))
    (.setKeyStorePassword context (options :key-password))
    (cond
      (string? (options :truststore))
      (.setTrustStorePath context (options :truststore))
      (instance? java.security.KeyStore (options :truststore))
      (.setTrustStore context ^java.security.KeyStore (options :truststore)))
    (when (options :trust-password)
      (.setTrustStorePassword context (options :trust-password)))
    (case (options :client-auth)
      :need (.setNeedClientAuth context true)
      :want (.setWantClientAuth context true)
      nil)
    context))

(defn- ^ServerConnector ssl-connector [server options]
  (let [ssl-port     (options :ssl-port 443)
        http-factory (HttpConnectionFactory.
                      (doto (http-config options)
                        (.setSecureScheme "https")
                        (.setSecurePort ssl-port)
                        (.addCustomizer (SecureRequestCustomizer.))))
        ssl-factory  (SslConnectionFactory.
                      (ssl-context-factory options)
                      "http/1.1")]
    (doto (server-connector server ssl-factory http-factory)
      (.setPort ssl-port)
      (.setHost (options :host))
      (.setIdleTimeout (options :max-idle-time 200000)))))

(defn- ^ThreadPool create-threadpool [options]
  (let [pool (QueuedThreadPool. ^Integer (options :max-threads 50))]
    (.setMinThreads pool (options :min-threads 8))
    (when (:daemon? options false)
      (.setDaemon pool true))
    pool))

(defn- ^Server create-server [options]
  (let [server (Server. (create-threadpool options))]
    (.addConnector server (http-connector server options))
    (when (or (options :ssl?) (options :ssl-port))
      (.addConnector server (ssl-connector server options)))
    server))

(defn ^Server run-jetty
  "Start a Jetty webserver to serve the given handler according to the
  supplied options:

  :configurator         - a function called with the Jetty Server instance
  :port                 - the port to listen on (defaults to 80)
  :host                 - the hostname to listen on
  :join?                - blocks the thread until server ends (defaults to true)
  :daemon?              - use daemon threads (defaults to false)
  :ssl?                 - allow connections over HTTPS
  :ssl-port             - the SSL port to listen on (defaults to 443, implies
                          :ssl? is true)
  :keystore             - the keystore to use for SSL connections
  :key-password         - the password to the keystore
  :truststore           - a truststore to use for SSL connections
  :trust-password       - the password to the truststore
  :max-threads          - the maximum number of threads to use (default 50)
  :min-threads          - the minimum number of threads to use (default 8)
  :max-idle-time        - the maximum idle time in milliseconds for a connection
                          (default 200000)
  :client-auth          - SSL client certificate authenticate, may be set to
                          :need,:want or :none (defaults to :none)
  :send-date-header?    - add a date header to the response (default true)
  :output-buffer-size   - the response body buffer size (default 32768)
  :request-header-size  - the maximum size of a request header (default 8192)
  :response-header-size - the maximum size of a response header (default 8192)"
  [handler options]
  (let [server (create-server (dissoc options :configurator))]
    (doto server
      (.setHandler (proxy-handler handler)))
    (when-let [configurator (:configurator options)]
      (configurator server))
    (try
      (.start server)
      (when (:join? options true)
        (.join server))
      server
      (catch Exception ex
        (.stop server)
        (throw ex)))))

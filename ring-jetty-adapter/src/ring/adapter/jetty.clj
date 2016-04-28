(ns ring.adapter.jetty
  "A Ring adapter that uses the Jetty 9 embedded web server.

  Adapters are used to convert Ring handlers into running web servers."
  (:require [ring.util.servlet :as servlet])
  (:import [org.eclipse.jetty.server
            Handler
            Request
            Server
            ServerConnector
            ConnectionFactory
            HttpConfiguration
            HttpConnectionFactory
            SslConnectionFactory
            SecureRequestCustomizer]
           [org.eclipse.jetty.server.handler AbstractHandler ResourceHandler HandlerList]
           [org.eclipse.jetty.util.thread ThreadPool QueuedThreadPool]
           [org.eclipse.jetty.util.ssl SslContextFactory]
           [javax.servlet.http HttpServletRequest HttpServletResponse]))

(defn- ^AbstractHandler proxy-handler [handler]
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request response]
      (let [request-map  (servlet/build-request-map request)
            response-map (handler request-map)]
        (when response-map
          (servlet/update-servlet-response response response-map)
          (.setHandled base-request true))))))

(defn- ^ServerConnector server-connector [server & factories]
  (ServerConnector. server (into-array ConnectionFactory factories)))

(defn- ^HttpConfiguration http-config [options]
  (doto (HttpConfiguration.)
    (.setSendDateHeader (:send-date-header? options true))
    (.setOutputBufferSize (:output-buffer-size options 32768))
    (.setRequestHeaderSize (:request-header-size options 8192))
    (.setResponseHeaderSize (:response-header-size options 8192))
    (.setSendServerVersion (:send-server-version? options true))))

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
    (if-let [exclude-ciphers (options :exclude-ciphers)]
      (.addExcludeCipherSuites context (into-array String exclude-ciphers)))
    (if-let [exclude-protocols (options :exclude-protocols)]
      (.addExcludeProtocols context (into-array String exclude-protocols)))
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

(defn- ^ResourceHandler create-resource-handler [resource-base & welcome-page]
  (doto (ResourceHandler.)
    (.setDirectoriesListed true)
    (.setWelcomeFiles (into-array String welcome-page))
    (.setResourceBase resource-base)))

(defn- ^HandlerList create-handler-list [def-handler resource-handler]
  (doto (HandlerList.)
    (.setHandlers (into-array Handler [def-handler resource-handler]))))

(defn- ^Server create-server [options]
  (let [server (Server. (create-threadpool options))]
    (when (:http? options true)
      (.addConnector server (http-connector server options)))
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
  :http?                - listen on :port for HTTP traffic (defaults to true)
  :ssl?                 - allow connections over HTTPS
  :ssl-port             - the SSL port to listen on (defaults to 443, implies
                          :ssl? is true)
  :welcome-page-conf    - configures welcome-page like index.html
  :exclude-ciphers      - When :ssl? is true, exclude these cipher suites
  :exclude-protocols    - When :ssl? is true, exclude these protocols
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
  :response-header-size - the maximum size of a response header (default 8192)
  :send-server-version? - add Server header to HTTP response (default true)"
  [handler options]
  (let [server (create-server (dissoc options :configurator))]
    (if-let [welcome-conf (:welcome-page-conf options)]
      (doto server
        (.setHandler (create-handler-list (proxy-handler handler)
                                          (create-resource-handler (:resource-base welcome-conf) (:welcome-page welcome-conf)))))
      (doto server
        (.setHandler (proxy-handler handler))))
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

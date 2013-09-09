(ns ring.adapter.jetty
  "Adapter for the Jetty webserver."
  (:import (org.eclipse.jetty.server Server Request)
           (org.eclipse.jetty.server.handler AbstractHandler)
           (org.eclipse.jetty.server.nio SelectChannelConnector)
           (org.eclipse.jetty.server.ssl SslSelectChannelConnector)
           (org.eclipse.jetty.util.thread QueuedThreadPool)
           (org.eclipse.jetty.util.ssl SslContextFactory)
           (javax.servlet.http HttpServletRequest HttpServletResponse))
  (:require [ring.util.servlet :as servlet]))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request response]
      (let [request-map  (servlet/build-request-map request)
            response-map (handler request-map)]
        (when response-map
          (servlet/update-servlet-response response response-map)
          (.setHandled base-request true))))))

(defn- ssl-context-factory
  "Creates a new SslContextFactory instance from a map of options."
  [options]
  (let [context (SslContextFactory.)]
    (if (string? (options :keystore))
      (.setKeyStorePath context (options :keystore))
      (.setKeyStore context ^java.security.KeyStore (options :keystore)))
    (.setKeyStorePassword context (options :key-password))
    (when (options :truststore)
      (.setTrustStore context ^java.security.KeyStore (options :truststore)))
    (when (options :trust-password)
      (.setTrustStorePassword context (options :trust-password)))
    (case (options :client-auth)
      :need (.setNeedClientAuth context true)
      :want (.setWantClientAuth context true)
      nil)
    context))

(defn- ssl-connector
  "Creates a SslSelectChannelConnector instance."
  [options]
  (doto (SslSelectChannelConnector. (ssl-context-factory options))
    (.setPort (options :ssl-port 443))
    (.setHost (options :host))
    (.setMaxIdleTime (options :max-idle-time 200000))))

(defn- create-server
  "Construct a Jetty Server instance."
  [options]
  (let [connector (doto (SelectChannelConnector.)
                    (.setPort (options :port 80))
                    (.setHost (options :host))
                    (.setMaxIdleTime (options :max-idle-time 200000)))
        server    (doto (Server.)
                    (.addConnector connector)
                    (.setSendDateHeader true))]
    (when (or (options :ssl?) (options :ssl-port))
      (.addConnector server (ssl-connector options)))
    server))

(defn ^Server run-jetty
  "Start a Jetty webserver to serve the given handler according to the
  supplied options:

  :configurator - a function called with the Jetty Server instance
  :port         - the port to listen on (defaults to 80)
  :host         - the hostname to listen on
  :join?        - blocks the thread until server ends (defaults to true)
  :daemon?      - use daemon threads (defaults to false)
  :ssl?         - allow connections over HTTPS
  :ssl-port     - the SSL port to listen on (defaults to 443, implies :ssl?)
  :keystore     - the keystore to use for SSL connections
  :key-password - the password to the keystore
  :truststore   - a truststore to use for SSL connections
  :trust-password - the password to the truststore
  :max-threads  - the maximum number of threads to use (default 50)
  :min-threads  - the minimum number of threads to use (default 8)
  :max-idle-time  - the maximum idle time in milliseconds for a connection (default 200000)
  :client-auth  - SSL client certificate authenticate, may be set to :need,
                  :want or :none (defaults to :none)"
  [handler options]
  (let [^Server s (create-server (dissoc options :configurator))
        ^QueuedThreadPool p (QueuedThreadPool. ^Integer (options :max-threads 50))]
    (.setMinThreads p (options :min-threads 8))
    (when (:daemon? options false)
      (.setDaemon p true))
    (doto s
      (.setHandler (proxy-handler handler))
      (.setThreadPool p))
    (when-let [configurator (:configurator options)]
      (configurator s))
    (.start s)
    (when (:join? options true)
      (.join s))
    s))

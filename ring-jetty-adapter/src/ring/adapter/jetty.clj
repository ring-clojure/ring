(ns ring.adapter.jetty
  (:import (org.mortbay.jetty.handler AbstractHandler)
           (org.mortbay.jetty Server Request Response)
           (org.mortbay.jetty.bio SocketConnector)
           (org.mortbay.jetty.security SslSocketConnector)
           (javax.servlet.http HttpServletRequest HttpServletResponse))
  (:use (ring.util servlet)
        (clojure.contrib except)))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  (proxy [AbstractHandler] []
    (handle [target #^Request request response dispatch]
      (let [request-map  (build-request-map request)
            response-map (handler request-map)]
        (when response-map
          (update-servlet-response response response-map)
          (.setHandled request true))))))

(defn- add-ssl-connector!
  "Add an SslSocketConnector to a Jetty Server instance."
  [#^Server server options]
  (let [ssl-connector (SslSocketConnector.)]
    (doto ssl-connector
      (.setPort        (options :ssl-port 443))
      (.setKeystore    (options :keystore))
      (.setKeyPassword (options :key-password)))
    (when (options :truststore)
      (.setTruststore ssl-connector (options :truststore)))
    (when (options :trust-password)
      (.setTrustPassword ssl-connector (options :trust-password)))
    (.addConnector server ssl-connector)))

(defn- create-server
  "Construct a Jetty Server instance."
  [options]
  (let [connector (doto (SocketConnector.)
                    (.setPort (options :port 80))
                    (.setHost (options :host)))
        server    (doto (Server.)
                    (.addConnector connector)
                    (.setSendDateHeader true))]
    (when (or (options :ssl) (options :ssl-port))
      (add-ssl-connector! server options))
    server))

(defn #^Server run-jetty
  "Serve the given handler according to the options.
  Options:
    :configurator (Optional, function; called with the Server instance.)
    :port (Optional, Integer)
    :host (Optional, String)
    :join? (Optional, true by default. If false, don't block.)
    :ssl-port, :keystore, :key-password, :truststore, :trust-password"
  [handler options]
  (let [#^Server s (create-server (dissoc options :configurator))]
    (when-let [configurator (:configurator options)]
      ;; Optional additional configuration, such as
      ;; adding JMX.
      (configurator s))
    
    (doto s
      (.setHandler (proxy-handler handler))
      (.start))
    
    (when (:join options true)
      (.join s))
    
    ;; Finally, return the Server.
    s))

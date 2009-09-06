(ns ring.adapter.jetty
  (:import (org.mortbay.jetty.handler AbstractHandler)
           (org.mortbay.jetty Server)
           (org.mortbay.jetty.bio SocketConnector)
           (org.mortbay.jetty.security SslSocketConnector))
  (:use (ring.util servlet)
        (clojure.contrib except)))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  (proxy [AbstractHandler] []
    (handle [target request response dispatch]
      (let [request-map  (build-request-map request)
            response-map (handler request-map)]
        (update-servlet-response response response-map)
        (.setHandled request true)))))

(defn- add-ssl-connector!
  "Add an SslSocketConnector to a Jetty Server instance."
  [server options]
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

(defn run-jetty
  "Serve the given handler according to the options.
  Options:
    :port (Optional, Integer)
    :host (Optional, String)
    :ssl-port, :keystore, :key-password, :truststore, :trust-password"
  [handler options]
  (doto (create-server options)
    (.setHandler (proxy-handler handler))
    (.start)
    (.join)))

(ns ring.adapter.jetty-async
  "Adapter for the Jetty webserver, with async HTTP and WebSockets support."
  (:import (org.eclipse.jetty.websocket WebSocketFactory WebSocket WebSocket$Outbound)
           (org.eclipse.jetty.server.handler AbstractHandler)
           (org.eclipse.jetty.server Server Request Response)
           (org.eclipse.jetty.server.bio SocketConnector)
           (org.eclipse.jetty.server.ssl SslSocketConnector)
           (javax.servlet.http HttpServletRequest))
  (:require [ring.util.servlet :as servlet]))

(defn- proxy-websocket
  "Create a Jetty WebSocket implementation for the given Ring channel handler."
  [channel-handler]
  (let [outbound (atom nil)
        reactor
          (fn [{:keys [type data]}]
            (case type
              :disconnect
                (if-let [^WebSocket$Outbound out @outbound]
                  (.disconnect out))
              :message
                (let [^WebSocket$Outbound out @outbound]
                  (if out
                    (.sendMessage out (byte 0) ^String data)
                    (throw (Exception. "no connection"))))))
        channel (channel-handler reactor)]
    (proxy [WebSocket] []
      (onConnect [out]
        (swap! outbound (constantly out))
        (channel {:type :connect}))
      (onDisconnect []
        (swap! outbound (constantly nil))
        (channel {:type :disconnect}))
      (onMessage
        ([frame data]
          (channel {:type :message :data data}))
        ([frame bdata offset length]
          (throw (Exception. "not yet implemented")))))))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  (let [buffer-size 8192
        max-idle -1
        factory (WebSocketFactory. buffer-size)]
    (.setMaxIdleTime factory -1)
    (proxy [AbstractHandler] []
      (handle [target ^Request base-request ^HttpServletRequest request response]
        (let [request-map (servlet/build-request-map request)
              response-map (handler request-map)]
          (condp = (:async response-map)
            nil
              (do
                (servlet/update-servlet-response response response-map)
                (.setHandled base-request true))
            :http
              (let [reactor (:reactor response-map)
                    ac (.startAsync request)
                    send (fn [{:keys [type data]}]
                           (case type
                             :status
                               (servlet/set-status (.getResponse ac) data)
                             :headers
                               (servlet/set-headers (.getResponse ac) data)
                             :chunk
                               (let [writer (.getWriter (.getResponse ac))]
                                 (.println writer data)
                                 (.flush writer))
                             :close
                              (.complete ac)))
                    recv (reactor send)]
                (recv {:type :init}))
            :websocket
              (let [reactor (:reactor response-map)
                    websocket (proxy-websocket reactor)
                    origin (or (get-in request-map [:headers "origin"])
                               (get-in request-map [:headers "host"]))
                    protocol (get-in request-map [:headers "websocket-protocol"])]
                (.upgrade factory request response websocket origin protocol))))))))

(defn- add-ssl-connector!
  "Add an SslSocketConnector to a Jetty Server instance."
  [^Server server options]
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
    (when (or (options :ssl?) (options :ssl-port))
      (add-ssl-connector! server options))
    server))

(defn ^Server run-jetty-async
  "Serve the given handler according to the options.
  Options:
    :configurator   - A function called with the Server instance.
    :port
    :host
    :join?          - Block the caller: defaults to true.
    :ssl?           - Use SSL.
    :ssl-port       - SSL port: defaults to 443, implies :ssl?
    :keystore
    :key-password
    :truststore
    :trust-password"
  [handler options]
  (let [^Server s (create-server (dissoc options :configurator))]
    (when-let [configurator (:configurator options)]
      (configurator s))
    (doto s
      (.setHandler (proxy-handler handler))
      (.start))
    (when (:join? options true)
      (.join s))
    s))

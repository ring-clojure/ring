(ns ring.websocket
  "Protocols and utility functions for websocket support."
  (:refer-clojure :exclude [send])
  (:require [clojure.string :as str]
            [ring.websocket.protocols :as p])
  (:import [java.nio ByteBuffer]))

(extend-type clojure.lang.IPersistentMap
  p/Listener
  (on-open [m socket]
    (when-let [kv (find m :on-open)] ((val kv) socket)))
  (on-message [m socket message]
    (when-let [kv (find m :on-message)] ((val kv) socket message)))
  (on-pong [m socket data]
    (when-let [kv (find m :on-pong)] ((val kv) socket data)))
  (on-error [m socket throwable]
    (when-let [kv (find m :on-error)] ((val kv) socket throwable)))
  (on-close [m socket code reason]
    (when-let [kv (find m :on-close)] ((val kv) socket code reason)))
  p/PingListener
  (on-ping [m socket data]
    (when-let [kv (find m :on-ping)] ((val kv) socket data))))

(defprotocol TextData
  "A protocol for converting text data into a String."
  (->char-sequence [data]
    "Convert some data into a CharSequence, ready to be sent as a websocket
    text message."))

(defprotocol BinaryData
  "A protocol for converting binary data into a java.nio.ByteBuffer object."
  (->byte-buffer [data]
    "Convert some binary data into a java.nio.ByteBuffer, ready to be sent as
    a websocket binary message."))

(extend-protocol TextData
  CharSequence
  (->char-sequence [cs] cs))

(extend-protocol BinaryData
  (Class/forName "[B")
  (->byte-buffer [bs] (ByteBuffer/wrap bs))
  ByteBuffer
  (->byte-buffer [bb] bb))

(defn- encode-message [message]
  (cond
    (satisfies? TextData message)   (->char-sequence message)
    (satisfies? BinaryData message) (->byte-buffer message)
    :else (throw (ex-info "message is not a valid text or binary data type"
                          {:message message}))))

(defn open?
  "Returns true if the Socket is open, false otherwise."
  [socket]
  (boolean (p/-open? socket)))

(defn send
  "Sends text or binary data via a websocket, either synchronously or
  asynchronously with callback functions. A convenient wrapper for the -send and
  -send-async protocol methods."
  ([socket message]
   (p/-send socket (encode-message message)))
  ([socket message succeed fail]
   (p/-send-async socket (encode-message message) succeed fail)))

(defn ping
  "Sends a ping message via a websocket, with an optional byte array or
  ByteBuffer that may contain custom session data. A convenient wrapper for the
  -ping protocol method."
  ([socket]
   (p/-ping socket (ByteBuffer/allocate 0)))
  ([socket data]
   (p/-ping socket (->byte-buffer data))))

(defn pong
  "Sends an unsolicited pong message via a websocket, with an optional byte
  array or ByteBuffer that may contain custom session data. A convenient wrapper
  for the -pong protocol method."
  ([socket]
   (p/-pong socket (ByteBuffer/allocate 0)))
  ([socket data]
   (p/-pong socket (->byte-buffer data))))

(defn close
  "Closes the websocket, with an optional custom integer status code and reason
  string."
  ([socket]
   (p/-close socket 1000 "Normal Closure"))
  ([socket code reason]
   (p/-close socket code reason)))

(defn upgrade-request?
  "Returns true if the request map is a websocket upgrade request."
  [request]
  (let [headers (:headers request)]
    (and (.equalsIgnoreCase "upgrade" (get headers "connection"))
         (.equalsIgnoreCase "websocket" (get headers "upgrade")))))

(defn websocket-response?
  "Returns true if the response contains a websocket listener."
  [response]
  (contains? response ::listener))

(defn request-protocols
  "Returns a collection of websocket subprotocols from a request map."
  [request]
  (some-> (:headers request)
          (get "sec-websocket-protocol")
          (str/split #"\s*,\s*")))

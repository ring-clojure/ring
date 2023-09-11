(ns ring.websocket
  "Protocols and utility functions for websocket support."
  (:refer-clojure :exclude [send])
  (:import [java.nio ByteBuffer]))

(defprotocol Listener
  "A protocol for handling websocket events. The second argument is
  always an object that satisfies the Socket protocol."
  (on-open [listener socket]
    "Called when the websocket is opened.")
  (on-message [listener socket message]
    "Called when a message is received. The message may be a String or a
    ByteBuffer.")
  (on-pong [listener socket data]
    "Called when a pong is received in response to an earlier ping. The client
    may provide additional binary data, represented by the data ByteBuffer.")
  (on-error [listener socket throwable]
    "Called when a Throwable error is thrown.")
  (on-close [listener socket code reason]
    "Called when the websocket is closed, along with an integer code and a
    plaintext string reason for being closed."))

(extend-protocol Listener
  clojure.lang.IPersistentMap
  (on-open [m socket]
    (when-let [kv (find m :on-open)] ((val kv) socket)))
  (on-message [m socket message]
    (when-let [kv (find m :on-message)] ((val kv) socket message)))
  (on-pong [m socket data]
    (when-let [kv (find m :on-pong)] ((val kv) socket data)))
  (on-error [m socket throwable]
    (when-let [kv (find m :on-error)] ((val kv) socket throwable)))
  (on-close [m socket code reason]
    (when-let [kv (find m :on-close)] ((val kv) socket code reason))))

(defprotocol Socket
  "A protocol for sending data via websocket."
  (-open? [socket]
    "Returns true if the socket is open; false otherwise.")
  (-send [socket message]
    "Sends a String or ByteBuffer to the client via the websocket.")
  (-ping [socket data]
    "Sends a ping message to the client with a ByteBuffer of extra data.")
  (-pong [socket data]
    "Sends an unsolicited pong message to the client, with a ByteBuffer of extra
    data.")
  (-close [socket code reason]
    "Closes the socket with an integer status code, and a String reason."))

(defprotocol AsyncSocket
  "A protocol for sending data asynchronously via websocket. Intended for use
  with the Socket protocol."
  (-send-async [socket message succeed fail]
    "Sends a String or ByteBuffer to the client via the websocket. If it
    succeeds, the 'succeed' callback function is called with zero arguments. If
    it fails, the 'fail' callback function is called with the exception that was
    thrown."))

(defprotocol TextData
  "A protocol for converting text data into a String."
  (->string [data]
    "Convert some data into a String, ready to be sent as a websocket text
    message."))

(defprotocol BinaryData
  "A protocol for converting binary data into a java.nio.ByteBuffer object."
  (->byte-buffer [data]
    "Convert some binary data into a java.nio.ByteBuffer, ready to be sent as
    a websocket binary message."))

(extend-protocol TextData
  String
  (->string [s] s))

(extend-protocol BinaryData
  (Class/forName "[B")
  (->byte-buffer [bs] (ByteBuffer/wrap bs))
  ByteBuffer
  (->byte-buffer [bb] bb))

(defn- encode-message [message]
  (cond
    (satisfies? TextData message)   (->string message)
    (satisfies? BinaryData message) (->byte-buffer message)
    :else (throw (ex-info "message is not a valid text or binary data type"
                          {:message message}))))

(defn open? [socket]
  (boolean (-open? socket)))

(defn send
  "Sends text or binary data via a websocket, either synchronously or
  asynchronously with callback functions. A convenient wrapper for the -send and
  -send-async protocol methods."
  ([socket message]
   (-send socket (encode-message message)))
  ([socket message succeed fail]
   (-send-async socket (encode-message message) succeed fail)))

(defn ping
  "Sends a ping message via a websocket, with an optional byte array or
  ByteBuffer that may contain custom session data. A convenient wrapper for the
  -ping protocol method."
  ([socket]
   (-ping socket (ByteBuffer/allocate 0)))
  ([socket data]
   (-ping socket (->byte-buffer data))))

(defn pong
  "Sends an unsolicited pong message via a websocket, with an optional byte
  array or ByteBuffer that may contain custom session data. A convenient wrapper
  for the -pong protocol method."
  ([socket]
   (-pong socket (ByteBuffer/allocate 0)))
  ([socket data]
   (-pong socket (->byte-buffer data))))

(defn close
  "Closes the websocket, with an optional custom integer status code and reason
  string."
  ([socket]
   (-close socket 1000 "Normal Closure"))
  ([socket code reason]
   (-close socket code reason)))

(defn websocket-request?
  "Returns true if the request map expects a websocket response."
  [request]
  (let [headers (:headers request)]
    (and (.equalsIgnoreCase "upgrade" (get headers "connection"))
         (.equalsIgnoreCase "websocket" (get headers "upgrade")))))

(defn websocket-response?
  "Returns true if the response contains a websocket listener."
  [response]
  (contains? response ::listener))

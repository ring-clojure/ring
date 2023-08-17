(ns ring.websocket
  (:refer-clojure :exclude [send])
  (:import [java.nio ByteBuffer]))

(defprotocol Listener
  (on-open    [listener socket])
  (on-message [listener socket message])
  (on-pong    [listener socket data])
  (on-error   [listener socket throwable])
  (on-close   [listener socket code reason]))

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
  (-send  [socket message])
  (-ping  [socket data])
  (-pong  [socket data])
  (-close [socket status reason]))

(defprotocol TextData
  (->string [data]))

(defprotocol BinaryData
  (->byte-buffer [data]))

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

(defn send [socket message]
  (-send socket (encode-message message)))

(defn ping
  ([socket]
   (-ping socket (ByteBuffer/allocate 0)))
  ([socket data]
   (-ping socket (->byte-buffer data))))

(defn pong
  ([socket]
   (-pong socket (ByteBuffer/allocate 0)))
  ([socket data]
   (-pong socket (->byte-buffer data))))

(defn close
  ([socket]
   (-close socket 1000 "Normal Closure"))
  ([socket code reason]
   (-close socket code reason)))

(defn websocket-request? [request]
  (let [headers (:headers request)]
    (and (.equalsIgnoreCase "upgrade" (get headers "connection"))
         (.equalsIgnoreCase "websocket" (get headers "upgrade")))))

(defn websocket-response? [response]
  (contains? response ::listener))

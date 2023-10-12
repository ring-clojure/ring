(ns ring.websocket.protocols)

(defprotocol Listener
  "A protocol for handling websocket events. The second argument is always an
  object that satisfies the Socket protocol."
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

(defprotocol PingListener
  "A protocol for handling ping websocket events. The second argument is always
  always an object that satisfies the Socket protocol. This is separate from
  the Listener protocol as some APIs (for example Jakarta) don't support
  listening for ping events."
  (on-ping [listener socket data]
    "Called when a ping is received from the client. The client may provide
    additional binary data, represented by the data ByteBuffer."))

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

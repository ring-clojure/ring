# Ring Spec (1.5.1)

Ring is an abstraction layer for building HTTP server applications in
Clojure.

The specification is divided into two parts; a synchronous API, and an
asynchronous API. The synchronous API is simpler, but the asynchronous
API can be more performant.


## 1. Synchronous API

Ring is defined in terms of handlers, middleware, adapters, request
maps and response maps, each of which are described below.

## 1.1. Handlers

Ring handlers constitute the core logic of the web application.
Handlers are implemented as Clojure functions.

A synchronous handler takes 1 argument, a request map, and returns a
response map.

```clojure
(fn [request] response)
```

### 1.2. Middleware

Ring middlware augment the functionality of handlers. Middleware is
implemented as higher-order functions that take one or more handlers
and configuration options as arguments and return a new handler with
the desired additional behavior.

### 1.3. Adapters

Ring adapters are side-effectful functions that take a handler and a
map of options as arguments, and when invoked start a HTTP server.

```clojure
(run-adapter handler options)
```

Once invoked, adapters will receive HTTP requests, parse them to
construct a request map, and then invoke their handler with this
request map as an argument. Once the handler response with a response
map, the adapter will use it to construct and send an HTTP response to
the client.

### 1.4. Request Maps

A Ring request map represents a HTTP request, and contains the
following keys. Any key not marked as **required** may be omitted. Keys
marked as **deprecated** are there only for backward compatibility with
earlier versions of the specification.

| Key                 | Type                               | Required | Deprecated |
| ------------------- | ---------------------------------- | -------- | ---------- |
|`:body`              |`java.io.InputStream`               |          |            |
|`:character-encoding`|`String`                            |          | Yes        |
|`:content-length`    |`String`                            |          | Yes        |
|`:content-type`      |`String`                            |          | Yes        |
|`:headers`           |`{String String}`                   | Yes      |            |
|`:protocol`          |`String`                            | Yes      |            |
|`:query-string`      |`String`                            |          |            |
|`:remote-addr`       |`String`                            | Yes      |            |
|`:request-method`    |`Keyword`                           | Yes      |            |
|`:scheme`            |`Keyword`                           | Yes      |            |
|`:server-name`       |`String`                            | Yes      |            |
|`:server-port`       |`Integer`                           | Yes      |            |
|`:ssl-client-cert`   |`java.security.cert.X509Certificate`|          |            |
|`:uri`               |`String`                            | Yes      |            |

#### :body

An `InputStream` for the request body, if one is present.

#### :character-encoding [DEPRECATED]

Equivalent to the character encoding specified in the `Content-Type`
header. Deprecated key.

#### :content-length [DEPRECATED]

Equivalent to the `Content-Length` header, converted to an integer.
Deprecated key.

#### :content-type [DEPRECATED]

Equivalent to the media type in the `Content-Type` header. Deprecated
key.

#### :headers

A Clojure map of lowercased header name strings to corresponding
header value strings.

Where there are multiple headers with the same name, the adapter must
concatenate the values into a single string, using the ASCII `,`
character as a delimiter.

The exception to this is the `cookie` header, which should instead use
the ASCII `;` character as a delimiter.

#### :protocol

The protocol the request was made with, e.g. "HTTP/1.1".

#### :query-string

The query segment of the URI in the HTTP request. This includes
everything after the `?` character, but excludes the `?` itself.

#### :remote-addr

The IP address of the client or the last proxy that sent the request.

#### :request-method

The HTTP request method. Must be a lowercase keyword corresponding to
a HTTP request method, such as `:get` or `:post`.

#### :scheme

The transport protocol denoted in the scheme of the request URL. Must be
either: `:http`, `:https`, `:ws` or `:wss`.

#### :server-name

The resolved server name, or the server IP address, as a string.

#### :server-port

The port on which the request is being handled.

#### :ssl-client-cert

The SSL client certificate, if supplied.

#### :uri

The absolute path of the URI in the HTTP request. Must start with a `/`.

### 1.5. Response Maps

A Ring response map represents a HTTP response, and contains the
following keys. Any key not marked as **required** may be omitted.

| Key      | Type                                       | Required |
| -------- | ------------------------------------------ | -------- |
|`:body`   |`ring.core.protocols/StreamableResponseBody`|          |
|`:headers`|`{String String}` or `{String [String]}`    | Yes      |
|`:status` |`Integer`                                   | Yes      |

#### :body

A representation of the response body that must satisfy the
`ring.core.protocols/StreamableResponseBody` protocol.

```clojure
(defprotocol StreamableResponseBody
  (write-body-to-stream [body response output-stream]))
```

The `response` argument is the full Ring response map, and the
`output-stream` argument is a `java.io.OutputStream` instance.

The `ring.core.protocols` namespace provides default implementations for
the following types:

* `byte[]`
* `String`
* `clojure.lang.ISeq`
* `java.io.InputStream`
* `java.io.File`
* `nil`

#### :headers

A Clojure map of header name strings to either a string or a vector of
strings that correspond to the header value or values.

#### :status

The HTTP status code. Must be greater than or equal to 100, and less
than or equal to 599.


## 2. Asynchronous API

The asynchronous API builds upon the synchronous API. The differences
between the two APIs are described below.

### 2.1. Handlers

An asynchronous handler takes 3 arguments: a request map, a callback
function for sending a response and a callback function for raising an
exception. The response callback takes a response map as its
argument. The exception callback takes an exception as its
argument. The return value of the function is ignored.

```clojure
(fn [request respond raise]
  (respond response))
```

```clojure
(fn [request respond raise]
  (raise exception))
```

A handler function may simultaneously support synchronous and
asynchronous behavior by accepting both arities.

```clojure
(fn
  ([request]
    response)
  ([request respond raise]
    (respond response)))
```

### 2.2. Adapters

An adapter may support synchronous handlers, or asynchronous handlers,
or both. If it supports both, it should have an option to specify
which one to use at the time it is invoked.

For example:

```clojure
(run-adapter handler {:async? true})
```

## 3. Websockets

A HTTP request can be promoted into a websocket by means of an
""upgrade" header.

In this situation, a Ring handler may choose to respond with a
websocket response instead of a HTTP response.

### 3.1. Websocket Responses

A websocket response is a map that represents a WebSocket, and may be
returned from a handler in place of a response map.

```clojure
(fn [request]
  #:ring.websocket{:listener websocket-listener})
```

It may also be used from an asynchronous handler.

```clojure
(fn [request respond raise]
  (respond #:ring.websocket{:listener websocket-listener}))
```

A websocket response contains the following keys. Any key not marked as
**required** may be omitted.

| Key                      | Type                    | Required |
| ------------------------ | ----------------------- | -------- |
|`:ring.websocket/listener`|`ring.websocket/Listener`| Yes      |
|`:ring.websocket/protocol`|`String`                 |          |

#### :ring.websocket/listener

An event listener that satisfies the `ring.websocket.protocols/Listener`
protocol, as described in section 3.2.

#### :ring.websocket/protocol

An optional websocket subprotocol. Must be one of the values listed in
the `Sec-Websocket-Protocol` header on the request.

### 3.2. Websocket Listeners

A websocket listener must satisfy the
`ring.websocket.protocols/Listener` protocol:

```clojure
(defprotocol Listener
  (on-open    [listener socket])
  (on-message [listener socket message])
  (on-pong    [listener socket data])
  (on-error   [listener socket throwable])
  (on-close   [listener socket code reason]))
```

It *may* optionally satisfy the `ring.websocket.protocols/PingListener`
protocol:

```clojure
(defprotocol PingListener
  (on-ping [listener socket data]))
```

If the `PingListener` protocol is not satisifed, the adapter *must*
default to respond to each ping message with a corresponding pong
message that has the same data.

#### on-open

Called once when the websocket is *successfully* opened. Supplies a
`socket` argument that satisfies `ring.websocket.protools/Socket`,
described in section 3.3.

#### on-message

Called when a text or binary message frame is received from the client.
The `message` argument must be a `java.lang.CharSequence` or a
`java.nio.ByteBuffer` depending on whether the message is text or binary.

#### on-ping

Called when a "ping" frame is received from the client. The `data`
argument is a `java.nio.ByteBuffer` that contains optional client
session data.

If the user implements this method, they are responsible for sending
the return "pong" that the websocket protocol expects.

#### on-pong

Called when a "pong" frame is received from the client. The `data`
argument is a `java.nio.ByteBuffer` that contains optional client
session data.

#### on-error

Called when an error occurs. This may cause the websocket to be closed.
The `throwable` argument is a `java.lang.Throwable` that was generated
by the error.

#### on-close

Called once when the websocket is closed, either via a valid close
frame or by an abnormal disconnect of the underlying TCP connection.
Guaranteed to be called if and only if `on-open` was called, so may be
used for finalizing/cleanup logic. Takes an integer `code` and a string
`reason` as arguments.

### 3.3. Websocket Sockets

A socket must satisfy the `ring.websocket.protocols/Socket` protocol:

```clojure
(defprotocol Socket
  (-open? [socket])
  (-send  [socket message])
  (-ping  [socket data])
  (-pong  [socket data])
  (-close [socket code reason]))
```

It *may* optionally satisfy the `ring.websocket.protocols/AsyncSocket`
protocol:

```clojure
(defprotocol AsyncSocket
  (-send-async [socket message succeed fail]))
```

#### -open?

Returns a truthy or falsey value denoting whether the socket is
currently connected to the client.

#### -send

Sends a websocket message frame that may be a `java.lang.CharSequence`
(for text), or a `java.nio.ByteBuffer` (for binary).

#### -send-async

As above, but does not block and requires two callback functions:

- `succeed` is called with zero arguments when the send succeeds
- `fail` is called with a single `java.lang.Throwable` argument when the
  send fails

#### -ping

Sends a websocket ping frame with a `java.nio.ByteBuffer` of session
data (which may be empty).

#### -pong

Sends an unsolicited pong frame with a `java.nio.ByteBuffer` of session
data (which may be empty).

#### -close

Closes the websocket with the supplied integer code and reason string.

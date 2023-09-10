# Ring Spec (1.5-DRAFT)

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

A representation of the request body that must satisfy the
`ring.core.protocols/StreamableResponseBody` protocol.

```clojure
(defprotocol StreamableResponseBody
  (write-body-to-stream [body response output-stream]))
```

#### :headers

A Clojure map of lowercased header name strings to either a string or
a vector of strings that correspond to the header value or values.

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
"upgrade" header.

In this situation, a Ring handler may choose to respond with a
websocket response instead of a HTTP response.

### 3.1. Websocket Responses

A websocket response is a map that has the `:ring.websocket/listener`
key, which maps to a websocket listener, described in section 3.2.

```clojure
(fn [request]
  #:ring.websocket{:listener websocket-listener})
```

A websocket response may be returned from a synchronous listener, or
via the response callback of an asynchronous listener.

```clojure
(fn [request respond raise]
  (respond #:ring.websocket{:listener websocket-listener}))
```

### 3.2. Websocket Listeners

A websocket listener must satisfy the `ring.websocket/Listener`
protocol:

```clojure
(defprotocol Listener
  (on-open    [listener socket])
  (on-message [listener socket message])
  (on-pong    [listener socket data])
  (on-error   [listener socket throwable])
  (on-close   [listener socket code reason]))
```

The arguments are described as follows:

* `socket`    - described in section 3.3.
* `message`   - a `String` or `java.nio.ByteBuffer` containing a message
* `data`      - a `java.nio.ByteBuffer` containing pong data
* `throwable` - an error inheriting from `java.lang.Throwable`
* `code`      - an integer from 1000 to 4999
* `reason`    - a string describing the reason for closing the socket

### 3.3. Websocket Sockets

A socket must satisfy the `ring.websocket/Socket` protocol:

```clojure
(defprotocol Socket
  (-send  [socket message])
  (-ping  [socket data])
  (-pong  [socket data])
  (-close [socket status reason]))
```

The types of the arguments are the same as those described for the
`Listener` protocol.

It *may* optionally satisfy the `ring.websocket/AsyncSocket` protocol:

```clojure
(defprotocol AsyncSocket
  (-send-async [socket message succeed fail]))
```

Where `succeed` is a callback function that expects zero arguments, and
`fail` is a callback function expecting a single `java.lang.Throwable`
argument.

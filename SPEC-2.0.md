# Ring Spec (2.0-DRAFT)

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
following keys. Any key not marked as **required** may be omitted.

| Key                           | Type                               | Required |
| ----------------------------- | ---------------------------------- | -------- |
|`:ring.request/body`           |`ring.request/StreamableRequestBody`|          |
|`:ring.request/headers`        |`{String [String]}`                 |          |
|`:ring.request/method`         |`Keyword`                           | Yes      |
|`:ring.request/path`           |`String`                            |          |
|`:ring.request/protocol`       |`String`                            |          |
|`:ring.request/query`          |`String`                            |          |
|`:ring.request/remote-addr`    |`String`                            |          |
|`:ring.request/scheme`         |`Keyword`                           |          |
|`:ring.request/server-name`    |`String`                            |          |
|`:ring.request/server-port`    |`Integer`                           |          |
|`:ring.request/ssl-client-cert`|`java.security.cert.X509Certificate`|          |

#### :ring.request/body

A representation of the request body that satisfies the
`StreamableRequestBody` protocol.

```clojure
(defprotocol StreamableRequestBody
  (get-body-stream [body request]))
```

#### :ring.request/headers

A Clojure map of lowercased header name strings to a vector of
corresponding header value strings.

#### :ring.request/method

The HTTP request method. Must be a lowercase keyword corresponding to
a HTTP request method, such as `:get` or `:post`.

#### :ring.request/path

The absolute path of the URI in the HTTP request. Must start with a `/`.

#### :ring.request/protocol

The protocol the request was made with, e.g. "HTTP/1.1".

#### :ring.request/query

The query segment of the URI in the HTTP request. This includes
everything after the `?` character, but excludes the `?` itself.

#### :ring.request/remote-addr

The IP address of the client or the last proxy that sent the request.

#### :ring.request/scheme

The transport protocol denoted in the scheme of the request URL. Must be
either: `:http`, `:https`, `:ws` or `:wss`.

#### :ring.request/server-name

The port on which the request is being handled.

#### :ring.request/server-port

The resolved server name, or the server IP address, as a string.

#### :ring.request/ssl-client-cert

The SSL client certificate, if supplied.

### 1.5. Response Maps

A Ring response map represents a HTTP response, and contains the
following keys. Any key not marked as **required** may be omitted.

| Key                   | Type                                 | Required |
| --------------------- | ------------------------------------ | -------- |
|`:ring.request/body`   |`ring.response/StreamableResponseBody`|          |
|`:ring.request/headers`|`{String [String]}`                   |          |
|`:ring.request/status` |`Integer`                             | Yes      |

#### :ring.response/body

A representation of the request body that satisfies the
`StreamableResponseBody` protocol.

```clojure
(defprotocol StreamableResponseBody
  (write-body-to-stream [body response output-stream]))
```

#### :ring.response/headers

A Clojure map of lowercased header name strings to a vector of
corresponding header value strings.

#### :ring.response/status

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

```clojure
(run-adapter handler {:async? true})
```


## 3. Websockets

A HTTP request can be promoted into a websocket by means of an
"upgrade" header.

In this situation, a Ring handler may choose to respond with a
websocket response instead of a HTTP response.


### 3.1. Websocket Responses

A websocket response is a map that has the `:ring.websocket/handler`
key, which maps to a websocket handler function, described in section
3.2.

```clojure
(fn [request]
  #:ring.websocket{:handler websocket-handler})
```

A websocket response may be returned from a synchronous handler, or
via the response callback of an asynchronous handler.

```clojure
(fn [request respond raise]
  (respond #:ring.websocket{:handler websocket-handler}))
```

### 3.2. Websocket Handlers

A websocket handler is a side-effectful function that expects two
arguments: a socket and a event map, which are described in sections
3.3 and 3.4 respectively. The return value is ignored.

```clojure
(fn [socket event])
```

### 3.3. Websocket Sockets

A socket satisfies the `ring.websocket/Socket` protocol:

```clojure
(defprotocol Socket
  (send-binary [socket bytes])
  (send-text   [socket string])
  (send-close  [socket status reason]))
```

### 3.4. Websocket Events

A websocket event is a map with the following keys:

| Key                         | Type      | Required |
| --------------------------- | --------- | -------- |
|`:ring.websocket/binary-data`|`byte[]`   |          |
|`:ring.websocket/error`      |`Throwable`|          |
|`:ring.websocket/event`      |`Keyword`  | Yes      |
|`:ring.websocket/reason`     |`String`   |          |
|`:ring.websocket/status`     |`Integer`  |          |
|`:ring.websocket/text-data`  |`String`   |          |

#### :ring.websocket/binary-data

A byte array representing the binary data of a websocket `:message`
event.

#### :ring.websocket/error

An exception deriving from `java.lang.Throwable` that defines the
error of a `:error` event.

#### :ring.websocket/event

The type of event occurring. Must be either: `:open`, `:message`,
`:error` or `:close`.

#### :ring.websocket/reason

A reason phrase string associated with a `:close` event.

#### :ring.websocket/status

A status code integer associated with a `:close` event. Must be
greater than or equal to 1000, and less than or equal to 4999.

#### :ring.websocket/text-data

A byte array representing the text data of a websocket `:message`
event.

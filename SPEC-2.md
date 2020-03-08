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

A representation of the request body that must satisfy the
`StreamableRequestBody` protocol.

```clojure
(defprotocol StreamableRequestBody
  (-body-stream [body request]))
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

The resolved server name, or the server IP address, as a string.

#### :ring.request/server-port

The port on which the request is being handled.

#### :ring.request/ssl-client-cert

The SSL client certificate, if supplied.

### 1.5. Response Maps

A Ring response map represents a HTTP response, and contains the
following keys. Any key not marked as **required** may be omitted.

| Key                    | Type                                 | Required |
| ---------------------- | ------------------------------------ | -------- |
|`:ring.response/body`   |`ring.response/StreamableResponseBody`|          |
|`:ring.response/headers`|`{String [String]}`                   |          |
|`:ring.response/status` |`Integer`                             | Yes      |

#### :ring.response/body

A representation of the request body that must satisfy the
`StreamableResponseBody` protocol.

```clojure
(defprotocol StreamableResponseBody
  (-write-body-to-stream [body response output-stream]))
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
  (on-error   [listener socket throwable])
  (on-close   [listener socket code reason]))
```

The arguments are described as follows:

* `socket`    - described in section 3.3.
* `message`   - a string or byte array containing a text/binary message
* `throwable` - an error inheriting from `java.lang.Throwable`
* `code`      - an integer from 1000 to 4999
* `reason`    - a string describing the reason for closing the socket

### 3.3. Websocket Sockets

A socket must satisfy the `ring.websocket/BaseSocket` protocol:

```clojure
(defprotocol BaseSocket
  (-send  [socket message])
  (-close [socket status reason]))
```

It *may* also satisfy the `ring.websocket/AsyncSocket` protocol:

```clojure
(defprotocol AsyncSocket
  (-send-async [socket message callback]))
```

## 4. HTTP/2 Server Push

If the adapter in use supports HTTP/2, an asynchronous handler may
optionally respond with a push map.

If the adapter does not support HTTP/2, any push map response should
be ignored.

### 4.1. Usage in Handlers

A push map is delivered via a call to the response callback passed to
the handler. A handler may respond with zero or more push maps, but
must respond with one and only one response map.

```clojure
(fn [request respond raise]
  (respond push1)
  (respond push2)
  (respond response))
```

### 4.2. Push Maps

A push map represents a server push, and contains the following keys.
Any key not marked as **required** may be omitted.

| Key                | Type              | Required |
| ------------------ | ----------------- | -------- |
|`:ring.push/headers`|`{String [String]}`|          |
|`:ring.push/method` |`Keyword`          |          |
|`:ring.push/path`   |`String`           | Yes      |
|`:ring.push/query`  |`String`           |          |

#### :ring.push/headers

A Clojure map of lowercased header name strings to a vector of
corresponding header value strings.

#### :ring.push/method

The HTTP request method. Must be a lowercase keyword corresponding to
a HTTP request method, such as `:get` or `:post`.

#### :ring.push/path

The path of the server push. May be a relative or absolute path.

#### :ring.push/query

The query segment that follows the path. This includes everything
after the `?` character, but excludes the `?` itself.

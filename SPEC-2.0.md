# Ring Spec (2.0-DRAFT)

Ring is defined in terms of handlers, middleware, adapters, request
maps and response maps, each of which are described below.

## 1. Synchronous API

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

#### `:ring.request/body` → `ring.request/StreamableRequestBody`

A representation of the request body that satisfies the
`StreamableRequestBody` protocol.

```clojure
(defprotocol StreamableRequestBody
  (get-input-stream [body request]))
```

#### `:ring.request/headers` → `{String [String]}`

A Clojure map of downcased header name Strings to a vector of
corresponding header value Strings.

#### `:ring.request/method` → `Keyword` (required)

The HTTP request method. Must be a lowercase keyword corresponding to
a HTTP request method, such as `:get` or `:post`.

#### `:ring.request/path` → `String`

The absolute path of the URI in the HTTP request. Must start with a `/`.

#### `:ring.request/protocol` → `String`

The protocol the request was made with, e.g. "HTTP/1.1".

#### `:ring.request/query` → `String`

The query segment of the URI in the HTTP request. This includes
everything after the `?` character, but excludes the `?` itself.

#### `:ring.request/remote-addr` → `String`

The IP address of the client or the last proxy that sent the request.

#### `:ring.request/scheme` → `Keyword`

The transport protocol denoted in the scheme of the request URL. Must be
either `:http` or `:https`.

#### `:ring.request/server-name` → `String`

The port on which the request is being handled.

#### `:ring.request/server-port` → `Integer`

The resolved server name, or the server IP address.

#### `:ring.request/ssl-client-cert` → `java.security.cert.X509Certificate`

The SSL client certificate, if supplied.

### 1.5. Response Maps

A Ring response map represents a HTTP response, and contains the
following keys. Any key not marked as **required** may be omitted.

#### `:ring.response/body` → `ring.response/StreamableResponseBody`

A representation of the request body that satisfies the
`StreamableResponseBody` protocol.

```clojure
(defprotocol StreamableResponseBody
  (write-body-to-stream [body response output-stream]))
```

#### `:ring.response/headers` → `{String [String]}`

A Clojure map of downcased header name Strings to a vector of
corresponding header value Strings.

#### `:ring.response/status` → `Integer` (required)

The HTTP status code. Must be greater than or equal to 100, and less
than or equal to 599.


## 2. Asynchronous API

### 2.1 Handlers

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

### 2.2 Adapters

An adapter may support synchronous handlers, or asynchronous handlers,
or both. If it supports both, it should have an option to specify
which one to use at the time it is invoked.

```clojure
(run-adapter handler {:async? true})
```

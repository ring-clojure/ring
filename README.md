# Ring

Ring is a Clojure web applications library inspired by Python's WSGI and Ruby's Rack. By abstracting the details of HTTP into a simple, unified API, Ring allows web applications to be constructed of modular components that can be shared among a variety of applications, web servers, and web frameworks.

The `SPEC` file at the root of this distribution for provides a complete description of the Ring interface.

## Synopsis

A "Hello World" Ring handler:

    (use 'ring.adapter.jetty)
    
    (defn app [req]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    "Hello World from Ring"})
    
    (run-jetty app {:port 8080})

Adding simple middleware:

    (defn wrap-upcase [app]
      (fn [req]
        (let [orig-resp (app req)]
          (assoc orig-resp :body (.toUpperCase (:body orig-resp))))))
    
    (def upcase-app (wrap-upcase app))
    
    (run-jetty upcase-app {:port 8080})

## Quick Start

To see some working examples, first pull in Ring's dependencies using [Leiningen](http://github.com/technomancy/leiningen):

    $ lein deps

To see a live "Hello World" Ring app, run:

    $ clj example/hello_world.clj

Now visit `http://localhost:8080/` in your browser; the Ring app will respond to your request with a simple HTML page indicating the time of day.

Note that your `clj` script needs to add the `src` directory and the jars in `lib` to your classpath.

To see a more sophisticated Ring app, run:

    $ clj example/wrapping.clj

* If you request `http://localhost:8080/` in your browser the `ring.handler.dump` handler will respond with an HTML page representing the request map that it received (see the `SPEC` for details on the request map).
* If you request `http://localhost:8080/clojure.png`, the `ring.middleware.file` middleware will detect that there is a `clojure.png` file in the app's `public` directory and return that image as a response.
* If you request `http://localhost:8080/error`, the app will produce an error that will be caught by the `ring.middleware.stacktrace` middleware, which will in turn return a readable stacktrace as the HTML response.
 
## Available Libraries

### ring-core

* `ring.middleware.file`: Serve static files out of a public directory.
* `ring.middleware.static`: Serve static files with specified prefixes out of a public directory.
* `ring.middleware.file-info`: Augment response headers with info about File responses.
* `ring.middleware.params`: Parse query and form params.
* `ring.middleware.multipart-params`: Parse multipart params.
* 'ring.middleware.keyword-params`: Convert string param keys to keywords.
* `ring.middleware.cookies`: Manage browser cookies.
* `ring.middleware.session`: Manage user sessions. Memory and cookie session stores are available by default.
* `ring.util.response`: Generate Ring responses.

### ring-devel

* `ring.handler.dump`: Dumps request maps as HTML responses for debugging.
* `ring.middleware.lint`: Lint requests and responses to ensure compliance with the Ring spec.
* `ring.middleware.reload`: Automatically reload selected libs before each request.
* `ring.middleware.stacktrace`: Catch exceptions and displays readable stacktraces for debugging.

### ring-servlet

* `ring.util.servlet`: Utilities for interfacing with Java Servlets.

###  ring-jetty-adapter

* `ring.adapter.jetty`: Adapter for the Jetty webserver.

### ring-httpcore-adapter

* `ring.adapter.httpcore`: Adapter for the Apache HttpCore webserver. 

## Leiningen Usage

To include one of the above libraries in your Leiningen project, for example `ring-core`, add the following to your `:dependences`:

    [ring/ring-core "0.2.0-RC1"]

or to depend on all of the available Ring libraries, add:

    [ring/ring "0.2.0-RC1"]

## Development

Ring is being actively developed; you can track its progress and contribute at the project's [GitHub page](http://github.com/mmcgrana/ring) and [Google Group](http://groups.google.com/group/ring-clojure).

To run the Ring unit tests, first navigate to the appropriate project and then:

    $ lein deps
    $ lein test

## Thanks

This project borrows heavily from Ruby's Rack and Python's WSGI; thanks to those communities for their work.

## License

Copyright (c) 2009-2010 Mark McGranaghan and released under an MIT license.

Clojure logo by Tom Hickey.

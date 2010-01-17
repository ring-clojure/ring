Ring is a Clojure web applications library inspired by Python's WSGI and Ruby's Rack. By abstracting the details of HTTP into a simple, unified API, Ring allows web applications to be constructed of modular components that can be shared among a variety of applications, web servers, and web frameworks.

The `SPEC` file at the root of this distribution for provides a complete description of the Ring interface.

Examples
--------

A Ring handler:

    (use 'ring.adapter.jetty)
    
    (defn app [req]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    "Hello World from Ring"})
    
    (run-jetty app {:port 8080})

Adding simple middleware:

    (defn with-upcase [app]
      (fn [req]
        (let [orig-resp (app req)]
          (assoc orig-resp :body (.toUpperCase (:body orig-resp))))))
    
    (def upcase-app (with-upcase app))
    
    (run-jetty upcase-app {:port 8080})

Quick Start
-----------

First, pull in Ring's dependencies using [Leiningen](http://github.com/technomancy/leiningen):

    $ lein deps

To see a live "Hello World" Ring app, run:

    $ clj src/ring/example/hello_world.clj

Now visit `http://localhost:8080/` in your browser; the Ring app will respond to your request with a simple HTML page indicating the time of day.

Note that your `clj` script needs to add the `src` directory and the jars in `lib` to your classpath.

To see a more sophisticated Ring app, run:

    $ clj src/ring/example/wrapping.clj

* If you request `http://localhost:8080/` in your browser the `ring.handler.dump` handler will respond with an HTML page representing the request map that it received (see the `SPEC` for details on the request map).
* If you request `http://localhost:8080/clojure.png`, the `ring.middleware.file` middleware will detect that there is a `clojure.png` file in the app's `public` directory and return that image as a response.
* If you request `http://localhost:8080/error`, the app will produce an error that will be caught by the `ring.middleware.stacktrace` middleware, which will in turn return a readable stacktrace as the HTML response.
 
Included Libs
-------------

* `ring.adapter.jetty`: Adapter for the Jetty webserver.
* `ring.adapter.httpcore`: Adapter for the Apache HttpCore webserver. 
* `ring.middleware.file`: Middleware that serves static files out of a public directory.
* `ring.middleware.file-info`: Middleware that augments response headers with info about File responses.
* `ring.middleware.lint`: Linter for the Ring interface, ensures compliance with the Ring spec.
* `ring.middleware.reload`: Middleware to automatically reload selected libs before each requests, minimizing server restarts.
* `ring.middleware.stacktrace`: Middleware that catches exceptions and displays readable stacktraces for debugging.
* `ring.middleware.static`: Middleware that serves static files with specified prefixes out of a public directory.
* `ring.handler.dump`: Handler that dumps request maps as HTML responses for debugging.
* `ring.util.servlet`: Utilities for interfacing with Java Servlets.
* `ring.example.*`: Various example Ring apps.

Development
-----------

Ring is being actively developed; you can track its progress and contribute at the project's [GitHub](http://github.com/mmcgrana/ring) page.

To run all the Ring unit tests:
  
    $ clj test/ring/run.clj

Thanks
------

This project borrows heavily from Ruby's Rack and Python's WSGI, and I thank the communities developing and supporting those projects.

License
-------

Copyright (c) 2009 Mark McGranaghan and released under an MIT license.

Clojure logo by Tom Hickey.
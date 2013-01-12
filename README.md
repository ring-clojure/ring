# Ring

Ring is a Clojure web applications library inspired by Python's WSGI
and Ruby's Rack. By abstracting the details of HTTP into a simple,
unified API, Ring allows web applications to be constructed of modular
components that can be shared among a variety of applications, web
servers, and web frameworks.

The [SPEC][1] file at the root of this distribution provides a
complete description of the Ring interface.

[1]: https://github.com/ring-clojure/ring/blob/master/SPEC

## Libraries

* ring-core - essential functions for handling parameters, cookies and more
* ring-devel - functions for developing and debugging Ring applications
* ring-servlet - construct Java servlets from Ring handlers
* ring-jetty-adapter - a Ring adapter that uses the Jetty webserver

## Installation

To include one of the above libraries, for example `ring-core`, add
the following to your `:dependencies`:

    [ring/ring-core "1.1.7"]

To include all of them:

    [ring "1.1.7"]

## Documentation

* [Wiki](https://github.com/ring-clojure/ring/wiki)
* [API docs](http://ring-clojure.github.com/ring)

## Community

* [Google group](http://groups.google.com/group/ring-clojure)

## Thanks

This project borrows heavily from Ruby's Rack and Python's WSGI;
thanks to those communities for their work.

## License

Copyright (c) 2009-2012 Mark McGranaghan and released under an MIT license.

# Ring [![Build Status](https://github.com/ring-clojure/ring/actions/workflows/test.yml/badge.svg)](https://github.com/ring-clojure/ring/actions/workflows/test.yml)

Ring is a Clojure web applications library inspired by Python's WSGI
and Ruby's Rack. By abstracting the details of HTTP into a simple,
unified API, Ring allows web applications to be constructed of modular
components that can be shared among a variety of applications, web
servers, and web frameworks.

The [SPEC.md][1] file at the root of this distribution provides a
complete description of the Ring interface. The [Wiki][2] contains
more in-depth documentation on how to use Ring.

[1]: https://github.com/ring-clojure/ring/blob/master/SPEC.md
[2]: https://github.com/ring-clojure/ring/wiki

## Libraries

* `ring/ring` - meta-package containing all relevant dependencies
* `ring/ring-core` - core functions and middleware for Ring handlers,
  requests and responses
* `org.ring-clojure/ring-websocket-protocols` - contains only the protocols
  necessary for WebSockets
* `ring/ring-devel` - functions for developing and debugging Ring
  applications
* `ring/ring-servlet` - construct legacy Java Servlets (≤ 4.0) from Ring
  handlers
* `org.ring-clojure/ring-jakarta-servlet` construct
  [Jakarta Servlets][3] (≥ 5.0) from Ring handlers
* `ring/ring-jetty-adapter` - a Ring adapter that uses an embedded
  [Jetty][4] web server

[3]: https://projects.eclipse.org/projects/ee4j.servlet
[4]: https://eclipse.dev/jetty/

## Installation

To include one of the above libraries, for instance `ring-core`, add
the following dependency to your `deps.edn` file:

    ring/ring-core {:mvn/version "1.11.0-RC2"}

Or to your Leiningen project file:

    [ring/ring-core "1.11.0-RC2"]

## Documentation

* [Wiki](https://github.com/ring-clojure/ring/wiki)
* [API docs](https://ring-clojure.github.io/ring/)

## Contributing

Please read [CONTRIBUTING.md][5] before submitting a pull request.

[5]: https://github.com/ring-clojure/ring/blob/master/CONTRIBUTING.md

## Thanks

This project borrows heavily from Ruby's Rack and Python's WSGI;
thanks to those communities for their work. Thanks also go to the many
individuals who have contributed to Ring's code and documentation over
the years.

## License

Copyright © 2009-2023 Mark McGranaghan, James Reeves & contributors.

Released under the MIT license.

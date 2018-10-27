# Ring

[![Build Status](https://travis-ci.org/ring-clojure/ring.svg?branch=master)](https://travis-ci.org/ring-clojure/ring)

Ring is a Clojure web applications library inspired by Python's WSGI
and Ruby's Rack. By abstracting the details of HTTP into a simple,
unified API, Ring allows web applications to be constructed of modular
components that can be shared among a variety of applications, web
servers, and web frameworks.

The [SPEC][1] file at the root of this distribution provides a
complete description of the Ring interface.

[1]: https://github.com/ring-clojure/ring/blob/master/SPEC

## Upgrade Notice

From version 1.2.1 onward, the ring/ring-core package no longer comes
with the `javax.servlet/servlet-api` package as a dependency (see
issue [#89][2]).

If you are using the `ring/ring-core` namespace on its own, you may
run into errors when executing tests or running alternative adapters.
To resolve this, include the following dependency in your dev profile:

    [javax.servlet/servlet-api "2.5"]

[2]: https://github.com/ring-clojure/ring/pull/89

## Libraries

* ring-core - essential functions for handling parameters, cookies and more
* ring-devel - functions for developing and debugging Ring applications
* ring-servlet - construct Java servlets from Ring handlers
* ring-jetty-adapter - a Ring adapter that uses the Jetty webserver

## Installation

To include one of the above libraries, for example `ring-core`, add
the following to your `:dependencies`:

    [ring/ring-core "1.7.1"]

To include all of them:

    [ring "1.7.1"]

## Documentation

* [Wiki](https://github.com/ring-clojure/ring/wiki)
* [API docs](http://ring-clojure.github.com/ring)

## Community

* [Google group](http://groups.google.com/group/ring-clojure)

## Contributing

Please see [CONTRIBUTING.md][3].

[3]: https://github.com/ring-clojure/ring/blob/master/CONTRIBUTING.md

## Thanks

This project borrows heavily from Ruby's Rack and Python's WSGI;
thanks to those communities for their work.

## License

Copyright © 2009-2018 Mark McGranaghan, James Reeves & contributors.

Released under the MIT license.

## 0.2.3 (2010-06-17)

* Code updated to be more Clojure 1.2 compatible
* Fixed bug in r.m.flash that was wiping out the session
* Added If-Modified-Since support to r.m.file-info
* Added ring.util.response/header
* Added :root key to r.m.session as a shortcut to cookie path attribute
* Updated ring-devel to use Hiccup instead of clj-html
* Session cookie attributes can now be set by adding a :session-cookie-attrs key to the response.

## 0.2.2 (2010-05-16)

* Introduce middleware for session flash
* Cookie middleware made to work for browsers that don't follow cookie RFC (which is most of them)

## 0.2.1 (2010-05-05)

* Depend on javax.servlet instead of org.mortbay.jetty for Servlet API artifact

## 0.2.0 (2010-03-28)

* Distribute Ring as separate Maven artifacts: `ring-core`, `ring-servlet`, `ring-devel`, `ring-jetty-adapter`, and `ring-http-core-adapter`
* The `ring` artifact now just depends on all of these granular artifacts
* Build with Leiningen
* Test with `clojure.test`
* Depend only on stable point-released libraries
* Introduce new middlewares for params, cookies, sessions
* Intro new utils for encoding/decoding, forming responses, and unit testing
* No longer require namespacing of request and response keys
* More documentation, including autodocs
* Various bugfixes

## 0.1.0 (2009-09-06)

* First numbered Ring release
* Adopt ring.{handler,middleware,adapter,util}.* namespace framework

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

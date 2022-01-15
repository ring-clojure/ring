## 1.9.5 (2022-01-15)

* Updated Jetty to 9.4.44.v20210927 (#453)
* Improved performance of params middleware (#446)

## 1.9.4 (2021-07-17)

* Updated Jetty to 9.4.42.v20210604 (#442)
* Updated Commons-IO to 2.10.0
* Updated Crypto-Random to 1.2.1

## 1.9.3 (2021-04-26)

* Fixed missing arity on async `OutputStream` (#436)
* Updated Jetty to 9.4.40.v20210413

## 1.9.2 (2021-03-20)

* Updated Jetty to 9.4.38.v20210224 (#433)
* Fixed reflection warning (#432)

## 1.9.1 (2021-02-17)

* Updated Ring-Codec dependency to 1.1.3
* Updated Jetty to 9.4.36.v20210114

## 1.9.0 (2021-02-03)

* Fixed automatic closing of response stream on exception (#420)
* Changed woff media type to `font/woff` & added woff2 media type (#421)
* Changed TTF media type to `font/ttf` (#426)
* Improved formatting in `ring.middleware.stacktrace` (#391)
* Improved performance of async responses (#428)
* Added `:exclude-ciphers` option to Jetty adapter (#405)
* Added `:exclude-protocols` option to Jetty adapter (#405)
* Added `:ssl-context` option to Jetty adapter (#412)
* Added `:async-timeout-handler` option to Jetty adapter (#410)
* Added `:keystore-scan-interval` option to Jetty adapter (#417)

## 1.8.2 (2020-10-06)

* Updated Jetty to 9.4.31.v20200723 (#411)

## 1.8.1 (2020-05-02)

* Deprecated string secret keys for cookie session stores
* Fixed `nil` bug in `wrap-resource` (#385)
* Fixed charset parsing not reading quoted values (#402)
* Fixed missing type hint in Jetty adapter (#401)
* Updated Jetty to 9.4.28.v2020040
* Added ring-bench benchmarking subproject

## 1.8.0 (2019-11-13)

* Fixed confusing exception on bad queue configuration (#354)
* Fixed poor content-type for `wrap-stacktrace` (#378)
* Fixed client cert support broken by deprecated class (#380)
* Fixed namespace reload order in `wrap-reload` (#377)
* Fixed second call of handler on exceptions (#365)
* Improved performance of `ring.request/content-type` (#332)
* Updated Jetty to 9.4.22.v20191022
* Updated Commons-FileUpload to 1.4
* Updated Ring-Codec to 1.1.2
* Updated ns-tracker to 0.4.0
* Added `:none` to `Same-Site` cookie header (#374)
* Added server connector type hint for GraalVM support (#381)
* Added support for byte array response bodies (#334)
* Removed clj-time as a mandatory dependency (#359)

## 1.7.1 (2018-10-27)

* Updated Jetty to 9.4.12.v20180830
* Fixed `wrap-resource` not working with a trailing slash

## 1.7.0 (2018-09-10)

* Functionally the same as 1.7.0-RC2.

## 1.7.0-RC2 (2018-08-25)

* Fixed multipart parameter reflection warning (#333)

## 1.7.0-RC1 (2018-05-23)

* Fixed `wrap-not-modified` to work per spec (#323)
* Updated minimum Clojure version to 1.7.0
* Updated Ring-Codec to 1.1.1 to fix handling of params without values (#312)
* Updated Jetty to 9.2.24.v20180105
* Updated Commons-IO to 2.6
* Updated clj-time to 0.14.3
* Added `:max-queued-requests` option to Jetty adapter (#289)
* Added `:thread-idle-timeout` option to Jetty adapter (#289)
* Added `:keystore-type` option to jetty adapter (#283)
* Added default media types for HLS and MPEG-DASH (#308)
* Added `:readers` option to session cookie store (#307)
* Added `ring.util.response/bad-request` (#315)
* Added `:prefer-handler?` option to `wrap-file` and `wrap-resource` (#321)

## 1.6.3 (2017-10-31)

* Fixed multipart field bug with byte-array-store (#301)
* Updated Commons-FileUpload to 1.3.3 to patch exploit (#310)

## 1.6.2 (2017-07-15)

* Fixed reflection warnings
* Set default `AsyncContext` timeout to zero (#299)
* Add `:async-timeout` option to override `AsyncContext` timeout

## 1.6.1 (2017-05-12)

* Fixed unreported ClosedChannelExceptions in async Jetty
* Updated Jetty to 9.2.21.v201701209

## 1.6.0 (2017-05-02)

* Functionally the same as 1.6.0-RC3.

## 1.6.0-RC3 (2017-04-18)

* Fixed erroneous type hint (#286)
* Fixed missing arguments in servlet functions (#287 and #288)
* Fixed missing `AsyncContext` complete after `sendError`
* Updated SPEC to clarify how to handle request headers with the same name

## 1.6.0-RC2 (2017-04-07)

* Fixed missing `:allow-symlinks?` option in `wrap-resource` (#284)

## 1.6.0-RC1 (2017-03-10)

* Fixed reflection warnings (#278)
* Added support for HTML `_charset_` field in `wrap-multipart-params` (#276)
* Added support for `SameSite` cookie attribute (#275)
* Added support for unicode in `wrap-keyword-params` (#271)
* Added preload for Cipher class to improve cookie store performance (#267)

## 1.6.0-beta7 (2017-01-10)

* Backported path traversal vulnerability fix from version 1.5.1
* Fixed reflection warnings in `ring.core.protocols`
* Stopped `wrap-stacktrace` HTML from overflowing

## 1.6.0-beta6 (2016-09-06)

* Updated Commons FileUpload to 1.3.2
* Fixed missing argument in `ring.middleware.sesson/session-request`
* Fixed intermittent 404 response in async Jetty adapter

## 1.6.0-beta5 (2016-08-10)

* Fixed bug in Jetty adapter that stopped async responses from completing
* Removed unnecessary tools.reader dependency

## 1.6.0-beta4 (2016-07-13)

* Renamed `ResponseBody` protocol to `StreamableResponseBody`
* Renamed `write-body` protocol method to `write-body-to-stream`
* Added `ring.util.response/get-charset` function
* Added response map to `write-body-to-stream` for character encoding of strings

## 1.6.0-beta3 (2016-07-09)

* Fixed responses closing when output stream is written asynchronously

## 1.6.0-beta2 (2016-07-07)

* Updated SPEC to accept three-arity asynchronous handlers
* Updated all middleware to work with asynchronous handlers
* Updated minimum servlet dependency in ring-servlet to 3.1
* Updated ring-servlet to use `javax.servlet.AsyncContext` with async handlers
* Updated ring-jetty-adapter to support async handlers when `:async?` option is true

## 1.6.0-beta1 (2016-06-23)

* Added `ring.core.protocols` namespace
* Updated SPEC to accept response bodies that satisfy `ring.core.protocols/ResponseBody`

## 1.5.1 (2017-01-10)

* Fixed path-traversal vulnerability in `resource-response` function

## 1.5.0 (2016-06-08)

* Added `:http?` option to Jetty adapter to allow HTTP to be turned off
* Added `:send-server-version?` option to Jetty adapter
* Added `:exclude-ciphers` and `:exclude-protocols` options to Jetty adapter
* Added a process listener function to `wrap-multipart-params`
* Made `file-response` prefer HTML files as indexes over other formats
* Made `wrap-reload` keep throwing compile exceptions, so errors aren't lost
* Fixed issue with `:recreate` metadata not being removed from sessions
* Fixed exception in `wrap-nested-params` on bad input
* Updated Ring-Codec to 1.0.1 to fix exceptions on bad URL encoding

## 1.4.0 (2015-07-09)

* Updated minimum Clojure version to 1.5.1
* Updated Jetty to 9.2.10 as Jetty 7 is at EOL (adapter now needs JDK 7 or later)
* Added four new Jetty 9 specific options to Jetty adapter
* Added :protocol key to the request map in the SPEC
* Added class :loader option to wrap-resource and resource-response
* Fixed lowercase header bug when working with non-english locales
* Added optional status argument to ring.util.response/redirect
* Added ring.util.response/resource-data multimethod
* Added functionality to regenerate sessions using :recreate metadata
* Fixed not-modified middleware affecting POSTs and 404s
* Added find-header and update-header to ring.util.response
* Fixed charset case sensitivity bug
* Updated clj-stacktrace, tools.reader, clj-time and Apache Commons FileUpload
* Fixed temporary filename leak in multipart-params middleware
* Updated wrap-file to accept java.io.File instances
* Updated ns-tracker to 0.3.0 to with more robust namespace parsing
* Fixed issue with clj-time cookie expiry dates on non-English locales

## 1.3.2 (2014-11-27)

* Ensure Jetty adapter threadpool is cleaned when server fails to start
* Fixed NPE in resource-response for directory resources in jar files
* Stopped ring.util.servlet/make-service being called every request
* Made wrap-nested-params safe to use with already nested params
* Fixed form field encoding in wrap-multipart-params
* Added mimetype for HTML5 application cache manifest

## 1.3.1 (2014-08-24)

* Support HEAD requests in ring.middleware.resource/resource-request
* Fix handling of nested parameters with names that include newlines

## 1.3.0 (2014-06-02)

* Deprecated :content-type, :content-length and :character-encoding keys in SPEC
* Removed deprecated keys from source code
* Added content-length, content-type and character-encoding to ring.util.request
* Added urlencoded-form? to ring.util.request
* Fixed 304 not-modified responses to set content-length header
* Added options to wrap-cookies to specify encoder and decoder functions
* Fixed wrap-head middleware when response is nil
* Cryptography improvements; RNG faster under Linux
* Jetty adapter accepts filepaths for :truststore option
* Added :min-threads, :max-queued and :max-idle-time options to Jetty adapter
* Fixed stacktrace middleware to handle assertion errors
* Added optional body to ring.util.response/created function
* Added :servlet-context-path to requests from servlet containers
* Added mimetypes for edn and dart
* Updated ns-tracker, clj-stacktrace and clj-time dependencies

## 1.2.2 (2014-03-13)

* Cookie middleware now adheres to RFC 6265
* Fix for wrap-nested-params middleware
* Update tools.reader version

## 1.2.1 (2013-10-28)

* Fix for resources in jar files created with Leiningen 2.3.3 or above
* Fix for UTF-8 characters in resource filenames
* javax.servlet now a provided dependency

## 1.2.0 (2013-07-08)

* Refactor of middleware to support async systems like Pedestal
* Added wrap-not-modified middleware
* Deprecated wrap-file-info middleware
* Added ring.util.request namespace
* file-response and resource-response include content-length and last-modifed headers
* Use of dedicated EDN reader for security
* Prettier wrap-stacktrace middleware
* Support for :path-info and :context keys
* Factored out encoding/decoding of data into ring-codec library
* Fixed bug with :session-cookie-attrs not working if cookie not set
* Fixed bug with last-modified dates on Windows
* Fixed case-sensitivity issues in middleware handling headers
* Added get-header, created and url-response to ring.util.response

## 1.1.8 (2013-01-20)

* Updated ns-tracker dependency to fix issue with Clojure 1.5.0
* Updated clj-stacktrace dependency to fix exception reporting

## 1.1.7 (2013-01-12)

* Secuity bug fix. See: http://goo.gl/DTRhn

## 1.1.6 (2012-09-22)

* Removed default charset being incorrectly set on images
* Fixed another bug in wrap-reload by updating ns-tracker version

## 1.1.5 (2012-09-03)

* Fixed hanging when compiling handler with wrap-multiparm-params middleware

## 1.1.4 (2012-09-02)

* Fixed bug in wrap-reload by updating ns-tracker version

## 1.1.3 (2012-08-22)

* Fixed wrap-multipart creating default store each request
* Fixed wrap-session :root option default overriding :cookie-attrs
* Fixed potential security issue where users could force a custom session cookie name

## 1.1.2 (2012-08-12)

* Fixed bug with content-type parameters in adapter and servlets
* Fixed bug with temp-file store spawning too many threads

## 1.1.1 (2012-06-16)

* Fixed bug with url-decoding "$"
* Fixed bug with trust-store password

## 1.1.0 (2012-04-23)

* Support for SSL client certificates in Jetty adapter
* Jetty adapter dependency upgraded to 7.6.1
* wrap-cookies support for Joda-Time objects in expires and max-age attributes
* Added wrap-head middleware
* wrap-file middleware has option to follow symlinks
* Added form-encode and form-decode to ring.util.codec
* Fixed url-encode and url-decode to handle "+" correctly
* Added ring.util.io namespace
* Deprecated ring.util.test namespace
* Hiccup ring-devel dependency upgraded to 1.0.0
* Added more functions to ring.util.response
* Default number of Jetty adapter threads is now 50
* Support for KeyStore instances in Jetty adapter
* Jetty configurator option now always applied last

## 1.0.2 (2012-01-25)

* Updated clj-stacktrace to 0.2.4 to fix swank-clojure issue

## 1.0.1 (2011-12-18)

* Workaround for [CLJ-885](http://dev.clojure.org/jira/browse/CLJ-885)

## 1.0.0 (2011-12-11)

* Multipart parameters with same name correctly create vector of values
* Fixed exception when resource-response is passed a directory
* wrap-reload middleware changed to act like wrap-reload-modified
* wrap-keyword-params ignores parameter names that cannot be keywords
* wrap-keyword-params can be safely applied multiple times
* Removed ring.middleware.static
* Servlet outputstream no longer explicitly closed and flushed
* Downgraded Jetty from 6.1.26 to 6.1.25 to solve socket issue
* Jetty SSL adapter respects the :host option
* Cookies can be set as http-only
* Fixed wrap-params for non-UTF8-encoded POST requests
* Fixed wrap-multipart-params bug that occurs in Clojure 1.3.0
* Better error reporting on invalid cookie attributes in wrap-cookies

## 0.3.11 (2011-07-14)

* Multipart parameter storage backends (temp-file and byte-array)
* Added redirect-after-post utility function
* Character encoding of response set from content type

## 0.3.10 (2011-06-28)

* Updated Hiccup to 0.3.6 for Clojure 1.3.0 compatibility

## 0.3.9 (2011-06-26)

* wrap-params no longer excepts on invalid urlencoded query string
* ring.util.servlet accepts multiple headers of the same name

## 0.3.8 (2011-04-23)

* resource-response returns File object when possible
* Added resource middleware
* Stacktrace middleware displays causes (nested exceptions)
* Bug fixes and refactor of stacktrace middleware

## 0.3.7 (2011-03-05)

* Lint middleware recognises ISeq as valid response body
* Added ring.util.mime-types namespace
* Added content-type middleware

## 0.3.6 (2011-02-16)

* Session and flash middleware handle nils without excepting
* Cookie session store compares HMAC with constant-time function

## 0.3.5 (2010-11-27)

* Context classloader now used for resource responses
* Removed HttpCore adapter from repository
* InputStream response body guaranteed to close
* Updated Jetty dependencies to 6.1.26

## 0.3.4 (2010-11-16)

* wrap-cookies no longer overwrites existing Set-Cookie header
* String response bodies no longer have extra newline

## 0.3.3 (2010-10-31)

* Added console logging for ring.handler.dump and ring.middleware.stacktrace
* Removed runtime dependency on clojure.contrib

## 0.3.2 (2010-10-11)

* Added nested-params middleware

## 0.3.1 (2010-09-26)

* Fixed multipart string encoding bug
* Memory sessions can now take a user-defined atom as an argument
* file-info middleware date checking improved
* Added option map to file middleware
* Jetty adapter :configurator option can now set Jetty handlers

## 0.3.0 (2010-09-19)

* Updated Clojure and Clojure-Contrib version to 1.2.0

## 0.2.6 (2010-09-09)

* Fixed non-string param values in keyword params middleware

## 0.2.5 (2010-07-06)

* Hopefully the last flash middleware fix we'll need
* Added ring.util.response/resource-response function

## 0.2.4 (2010-07-04)

* Fixed race condition in file-info middleware date parsing
* Forced US locale for file-info middleware date parsing
* Fixed NPE in multipart-params middleware when field is nil
* Fixed another flash middleware bug that was wiping out session data

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

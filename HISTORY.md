## 1.4.0 (TBD)

* Updated minimum Clojure version to 1.5.1
* Updated Jetty to 9.2.10 as Jetty 7 is at EOL (adapter now needs JDK 7 or later)
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

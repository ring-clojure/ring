{:namespaces
 ({:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.adapter.jetty-api.html",
   :name "ring.adapter.jetty",
   :doc "Adapter for the Jetty webserver."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.handler.dump-api.html",
   :name "ring.handler.dump",
   :doc "Reflect Ring requests into responses for debugging."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.content-type-api.html",
   :name "ring.middleware.content-type",
   :doc
   "Middleware for automatically adding a content type to response maps."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.cookies-api.html",
   :name "ring.middleware.cookies",
   :doc "Cookie manipulation."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.file-api.html",
   :name "ring.middleware.file",
   :doc "Static file serving."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.file-info-api.html",
   :name "ring.middleware.file-info",
   :doc "Augment Ring File responses."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.flash-api.html",
   :name "ring.middleware.flash",
   :doc
   "A session-based flash store that persists to the next request."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.keyword-params-api.html",
   :name "ring.middleware.keyword-params",
   :doc "Convert param keys to keywords."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.lint-api.html",
   :name "ring.middleware.lint",
   :doc "Lint Ring requests and responses."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.multipart-params-api.html",
   :name "ring.middleware.multipart-params",
   :doc "Parse multipart upload into params."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.nested-params-api.html",
   :name "ring.middleware.nested-params",
   :doc nil}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.params-api.html",
   :name "ring.middleware.params",
   :doc "Parse form and query params."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.reload-api.html",
   :name "ring.middleware.reload",
   :doc "Reload namespaces before requests."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.resource-api.html",
   :name "ring.middleware.resource",
   :doc "Middleware for serving static resources."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.session-api.html",
   :name "ring.middleware.session",
   :doc "Session manipulation."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.stacktrace-api.html",
   :name "ring.middleware.stacktrace",
   :doc
   "Catch exceptions and render web and log stacktraces for debugging."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.static-api.html",
   :name "ring.middleware.static",
   :doc
   "Static file serving, more selective than ring.middleware.file."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.util.codec-api.html",
   :name "ring.util.codec",
   :doc "Encoding and decoding utilities."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.util.mime-type-api.html",
   :name "ring.util.mime-type",
   :doc "Utility functions for finding out the mime-type of a file."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.util.response-api.html",
   :name "ring.util.response",
   :doc "Generate and augment Ring responses."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.util.servlet-api.html",
   :name "ring.util.servlet",
   :doc
   "Compatibility functions for turning a ring handler into a Java servlet."}
  {:source-url nil,
   :wiki-url "http://mmcgrana.github.com/ring/ring.util.test-api.html",
   :name "ring.util.test",
   :doc "Utilities for testing Ring components."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.multipart-params.byte-array-api.html",
   :name "ring.middleware.multipart-params.byte-array",
   :doc nil}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.multipart-params.temp-file-api.html",
   :name "ring.middleware.multipart-params.temp-file",
   :doc nil}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.session.cookie-api.html",
   :name "ring.middleware.session.cookie",
   :doc "Encrypted cookie session storage."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.session.memory-api.html",
   :name "ring.middleware.session.memory",
   :doc "In-memory session storage."}
  {:source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring/ring.middleware.session.store-api.html",
   :name "ring.middleware.session.store",
   :doc "Common session store objects and functions."}),
 :vars
 ({:arglists ([handler options]),
   :name "run-jetty",
   :namespace "ring.adapter.jetty",
   :source-url
   "http://github.com/mmcgrana/ring/blob/f2ca099a6102da4e080a29e0a00e208a152debba/ring-jetty-adapter/src/ring/adapter/jetty.clj#L48",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/f2ca099a6102da4e080a29e0a00e208a152debba/ring-jetty-adapter/src/ring/adapter/jetty.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.adapter.jetty-api.html#ring.adapter.jetty/run-jetty",
   :doc
   "Serve the given handler according to the options.\nOptions:\n  :configurator   - A function called with the Server instance.\n  :port\n  :host\n  :join?          - Block the caller: defaults to true.\n  :ssl?           - Use SSL.\n  :ssl-port       - SSL port: defaults to 443, implies :ssl?\n  :keystore\n  :key-password\n  :truststore\n  :trust-password",
   :var-type "function",
   :line 48,
   :file
   "/home/jim/Development/ring/./ring-jetty-adapter/src/ring/adapter/jetty.clj"}
  {:arglists ([req]),
   :name "handle-dump",
   :namespace "ring.handler.dump",
   :source-url
   "http://github.com/mmcgrana/ring/blob/aca341ead44df404768ddcbfab08762f45fd4a93/ring-devel/src/ring/handler/dump.clj#L44",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/aca341ead44df404768ddcbfab08762f45fd4a93/ring-devel/src/ring/handler/dump.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.handler.dump-api.html#ring.handler.dump/handle-dump",
   :doc
   "Returns a response tuple corresponding to an HTML dump of the request\nreq as it was recieved by this app.",
   :var-type "function",
   :line 44,
   :file
   "/home/jim/Development/ring/./ring-devel/src/ring/handler/dump.clj"}
  {:arglists ([handler & [opts]]),
   :name "wrap-content-type",
   :namespace "ring.middleware.content-type",
   :source-url
   "http://github.com/mmcgrana/ring/blob/929b165f60061505cd0a10fa06aca6799242e5ec/ring-core/src/ring/middleware/content_type.clj#L6",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/929b165f60061505cd0a10fa06aca6799242e5ec/ring-core/src/ring/middleware/content_type.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.content-type-api.html#ring.middleware.content-type/wrap-content-type",
   :doc
   "Middleware that adds a content-type header to the response if one is not\nset by the handler. Uses the ring.util.mime-type/ext-mime-type function to\nguess the content-type from the file extension in the URI. If no\ncontent-type can be found, it defaults to 'application/octet-stream'.\n\nAccepts the following options:\n  :mime-types - a map of filename extensions to mime-types that will be\n                used in addition to the ones defined in\n                ring.util.mime-types/default-mime-types",
   :var-type "function",
   :line 6,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/content_type.clj"}
  {:arglists ([handler]),
   :name "wrap-cookies",
   :namespace "ring.middleware.cookies",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.cookies-api.html#ring.middleware.cookies/wrap-cookies",
   :doc
   "Parses the cookies in the request map, then assocs the resulting map\nto the :cookies key on the request.",
   :var-type "function",
   :line 124,
   :file "ring/middleware/cookies.clj"}
  {:arglists ([app root-path & [opts]]),
   :name "wrap-file",
   :namespace "ring.middleware.file",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.file-api.html#ring.middleware.file/wrap-file",
   :doc
   "Wrap an app such that the directory at the given root-path is checked for a\nstatic file with which to respond to the request, proxying the request to the\nwrapped app if such a file does not exist.\n\nAn map of options may be optionally specified. These options will be passed\nto the ring.util.response/file-response function.",
   :var-type "function",
   :line 14,
   :file "ring/middleware/file.clj"}
  {:arglists ([]),
   :name "make-http-format",
   :namespace "ring.middleware.file-info",
   :source-url
   "http://github.com/mmcgrana/ring/blob/b38ad2f95548246a90aaba3ad7f5337c17e15c6a/ring-core/src/ring/middleware/file_info.clj#L16",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/b38ad2f95548246a90aaba3ad7f5337c17e15c6a/ring-core/src/ring/middleware/file_info.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.file-info-api.html#ring.middleware.file-info/make-http-format",
   :doc
   "Formats or parses dates into HTTP date format (RFC 822/1123).",
   :var-type "function",
   :line 16,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/file_info.clj"}
  {:arglists ([app & [mime-types]]),
   :name "wrap-file-info",
   :namespace "ring.middleware.file-info",
   :source-url
   "http://github.com/mmcgrana/ring/blob/b38ad2f95548246a90aaba3ad7f5337c17e15c6a/ring-core/src/ring/middleware/file_info.clj#L30",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/b38ad2f95548246a90aaba3ad7f5337c17e15c6a/ring-core/src/ring/middleware/file_info.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.file-info-api.html#ring.middleware.file-info/wrap-file-info",
   :doc
   "Wrap an app such that responses with a file a body will have corresponding\nContent-Type, Content-Length, and Last Modified headers added if they can be\ndetermined from the file.\nIf the request specifies a If-Modified-Since header that matches the last\nmodification date of the file, a 304 Not Modified response is returned.\nIf two arguments are given, the second is taken to be a map of file extensions\nto content types that will supplement the default, built-in map.",
   :var-type "function",
   :line 30,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/file_info.clj"}
  {:arglists ([handler]),
   :name "wrap-flash",
   :namespace "ring.middleware.flash",
   :source-url
   "http://github.com/mmcgrana/ring/blob/8a1550ee74bd376bd7ee056a4d39942a3131af39/ring-core/src/ring/middleware/flash.clj#L4",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/8a1550ee74bd376bd7ee056a4d39942a3131af39/ring-core/src/ring/middleware/flash.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.flash-api.html#ring.middleware.flash/wrap-flash",
   :doc
   "If a :flash key is set on the response by the handler, a :flash key with\nthe same value will be set on the next request that shares the same session.\nThis is useful for small messages that persist across redirects.",
   :var-type "function",
   :line 4,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/flash.clj"}
  {:arglists ([handler]),
   :name "wrap-keyword-params",
   :namespace "ring.middleware.keyword-params",
   :source-url
   "http://github.com/mmcgrana/ring/blob/5038dc228e4d5eb81809f5f82a33b1b8674ac79e/ring-core/src/ring/middleware/keyword_params.clj#L15",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/5038dc228e4d5eb81809f5f82a33b1b8674ac79e/ring-core/src/ring/middleware/keyword_params.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.keyword-params-api.html#ring.middleware.keyword-params/wrap-keyword-params",
   :doc
   "Middleware that converts the string-keyed :params map to one with keyword\nkeys before forwarding the request to the given handler.\nDoes not alter the maps under :*-params keys; these are left with strings.",
   :var-type "function",
   :line 15,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/keyword_params.clj"}
  {:arglists ([app]),
   :name "wrap-lint",
   :namespace "ring.middleware.lint",
   :source-url
   "http://github.com/mmcgrana/ring/blob/9a3cc92f60df9dbb08568f03f65e5fc7cc425522/ring-devel/src/ring/middleware/lint.clj#L84",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/9a3cc92f60df9dbb08568f03f65e5fc7cc425522/ring-devel/src/ring/middleware/lint.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.lint-api.html#ring.middleware.lint/wrap-lint",
   :doc
   "Wrap an app to validate incoming requests and outgoing responses\naccording to the Ring spec.",
   :var-type "function",
   :line 84,
   :file
   "/home/jim/Development/ring/./ring-devel/src/ring/middleware/lint.clj"}
  {:arglists ([]),
   :name "default-store",
   :namespace "ring.middleware.multipart-params",
   :source-url
   "http://github.com/mmcgrana/ring/blob/8dc40e8f38c59c9a1275991d7b7522b620b3a180/ring-core/src/ring/middleware/multipart_params.clj#L65",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/8dc40e8f38c59c9a1275991d7b7522b620b3a180/ring-core/src/ring/middleware/multipart_params.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.multipart-params-api.html#ring.middleware.multipart-params/default-store",
   :doc "Loads and returns a temporary file store.",
   :var-type "function",
   :line 65,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/multipart_params.clj"}
  {:arglists ([handler & [opts]]),
   :name "wrap-multipart-params",
   :namespace "ring.middleware.multipart-params",
   :source-url
   "http://github.com/mmcgrana/ring/blob/8dc40e8f38c59c9a1275991d7b7522b620b3a180/ring-core/src/ring/middleware/multipart_params.clj#L72",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/8dc40e8f38c59c9a1275991d7b7522b620b3a180/ring-core/src/ring/middleware/multipart_params.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.multipart-params-api.html#ring.middleware.multipart-params/wrap-multipart-params",
   :doc
   "Middleware to parse multipart parameters from a request. Adds the\nfollowing keys to the request map:\n  :multipart-params - a map of multipart parameters\n  :params           - a merged map of all types of parameter\n\nThis middleware takes an optional configuration map. Recognized keys are:\n\n  :encoding - character encoding to use for multipart parsing. If not\n              specified, uses the request character encoding, or \"UTF-8\"\n              if no request character encoding is set.\n\n  :store    - a function that stores a file upload. The function should\n              expect a map with :filename, content-type and :stream keys,\n              and its return value will be used as the value for the\n              parameter in the multipart parameter map. The default storage\n              function is the temp-file-store.",
   :var-type "function",
   :line 72,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/multipart_params.clj"}
  {:arglists ([param-name]),
   :name "parse-nested-keys",
   :namespace "ring.middleware.nested-params",
   :source-url
   "http://github.com/mmcgrana/ring/blob/8a7620c884b2719355a0db1870054b742dcfd06d/ring-core/src/ring/middleware/nested_params.clj#L3",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/8a7620c884b2719355a0db1870054b742dcfd06d/ring-core/src/ring/middleware/nested_params.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.nested-params-api.html#ring.middleware.nested-params/parse-nested-keys",
   :doc
   "Parse a parameter name into a list of keys using a 'C'-like index\nnotation. e.g.\n  \"foo[bar][][baz]\"\n  => [\"foo\" \"bar\" \"\" \"baz\"]",
   :var-type "function",
   :line 3,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/nested_params.clj"}
  {:arglists ([handler & [opts]]),
   :name "wrap-nested-params",
   :namespace "ring.middleware.nested-params",
   :source-url
   "http://github.com/mmcgrana/ring/blob/8a7620c884b2719355a0db1870054b742dcfd06d/ring-core/src/ring/middleware/nested_params.clj#L47",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/8a7620c884b2719355a0db1870054b742dcfd06d/ring-core/src/ring/middleware/nested_params.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.nested-params-api.html#ring.middleware.nested-params/wrap-nested-params",
   :doc
   "Middleware to converts a flat map of parameters into a nested map.\n\nUses the function in the :key-parser option to convert parameter names\nto a list of keys. Values in keys that are empty strings are treated\nas elements in a list. Defaults to using the parse-nested-keys function.\n\ne.g.\n  {\"foo[bar]\" \"baz\"}\n  => {\"foo\" {\"bar\" \"baz\"}}\n\n  {\"foo[]\" \"bar\"}\n  => {\"foo\" [\"bar\"]}",
   :var-type "function",
   :line 47,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/nested_params.clj"}
  {:arglists ([map key val]),
   :name "assoc-param",
   :namespace "ring.middleware.params",
   :source-url
   "http://github.com/mmcgrana/ring/blob/b97bd125d95e6dad35de23e6b3689c62961f3a4a/ring-core/src/ring/middleware/params.clj#L6",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/b97bd125d95e6dad35de23e6b3689c62961f3a4a/ring-core/src/ring/middleware/params.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.params-api.html#ring.middleware.params/assoc-param",
   :doc
   "Associate a key with a value. If the key already exists in the map,\ncreate a vector of values.",
   :var-type "function",
   :line 6,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/params.clj"}
  {:arglists ([handler & [opts]]),
   :name "wrap-params",
   :namespace "ring.middleware.params",
   :source-url
   "http://github.com/mmcgrana/ring/blob/b97bd125d95e6dad35de23e6b3689c62961f3a4a/ring-core/src/ring/middleware/params.clj#L54",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/b97bd125d95e6dad35de23e6b3689c62961f3a4a/ring-core/src/ring/middleware/params.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.params-api.html#ring.middleware.params/wrap-params",
   :doc
   "Middleware to parse urlencoded parameters from the query string and form\nbody (if the request is a urlencoded form). Adds the following keys to\nthe request map:\n  :query-params - a map of parameters from the query string\n  :form-params  - a map of parameters from the body\n  :params       - a merged map of all types of parameter\nTakes an optional configuration map. Recognized keys are:\n  :encoding - encoding to use for url-decoding. If not specified, uses\n              the request character encoding, or \"UTF-8\" if no request\n              character encoding is set.",
   :var-type "function",
   :line 54,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/params.clj"}
  {:arglists ([app reloadables]),
   :name "wrap-reload",
   :namespace "ring.middleware.reload",
   :source-url
   "http://github.com/mmcgrana/ring/blob/49d8adb2c3cdc0fff7dec68879882b2caa8e9193/ring-devel/src/ring/middleware/reload.clj#L4",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/49d8adb2c3cdc0fff7dec68879882b2caa8e9193/ring-devel/src/ring/middleware/reload.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.reload-api.html#ring.middleware.reload/wrap-reload",
   :doc
   "Wrap an app such that before a request is passed to the app, each namespace\nidentified by syms in reloadables is reloaded.\nCurrently this requires that the namespaces in question are being (re)loaded\nfrom un-jarred source files, as apposed to source files in jars or compiled\nclasses.",
   :var-type "function",
   :line 4,
   :file
   "/home/jim/Development/ring/./ring-devel/src/ring/middleware/reload.clj"}
  {:arglists ([handler root-path]),
   :name "wrap-resource",
   :namespace "ring.middleware.resource",
   :source-url
   "http://github.com/mmcgrana/ring/blob/e89404c4048bacc07cd5d8306afb3ade5862e02c/ring-core/src/ring/middleware/resource.clj#L6",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/e89404c4048bacc07cd5d8306afb3ade5862e02c/ring-core/src/ring/middleware/resource.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.resource-api.html#ring.middleware.resource/wrap-resource",
   :doc
   "Middleware that first checks to see whether the request map matches a static\nresource. If it does, the resource is returned in a response map, otherwise\nthe request map is passed onto the handler. The root-path argument will be\nadded to the beginning of the resource path.",
   :var-type "function",
   :line 6,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/resource.clj"}
  {:arglists ([handler] [handler options]),
   :name "wrap-session",
   :namespace "ring.middleware.session",
   :source-url
   "http://github.com/mmcgrana/ring/blob/f782316bdc3a5018be0efed0754f24b540e5925e/ring-core/src/ring/middleware/session.clj#L6",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/f782316bdc3a5018be0efed0754f24b540e5925e/ring-core/src/ring/middleware/session.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.session-api.html#ring.middleware.session/wrap-session",
   :doc
   "Reads in the current HTTP session map, and adds it to the :session key on\nthe request. If a :session key is added to the response by the handler, the\nsession is updated with the new value. If the value is nil, the session is\ndeleted.\n\nThe following options are available:\n  :store\n    An implementation of the SessionStore protocol in the\n    ring.middleware.session.store namespace. This determines how the\n    session is stored. Defaults to in-memory storage\n    (ring.middleware.session.store.MemoryStore).\n  :root\n    The root path of the session. Anything path above this will not\n    be able to see this session. Equivalent to setting the cookie's\n    path attribute. Defaults to \"/\".\n  :cookie-name\n    The name of the cookie that holds the session key. Defaults to\n    \"ring-session\"\n  :cookie-attrs\n    A map of attributes to associate with the session cookie. Defaults\n    to {}.",
   :var-type "function",
   :line 6,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/session.clj"}
  {:arglists ([handler]),
   :name "wrap-stacktrace",
   :namespace "ring.middleware.stacktrace",
   :source-url
   "http://github.com/mmcgrana/ring/blob/38131596c5a2a686399542b6bf3cc931b47298e3/ring-devel/src/ring/middleware/stacktrace.clj#L83",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/38131596c5a2a686399542b6bf3cc931b47298e3/ring-devel/src/ring/middleware/stacktrace.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.stacktrace-api.html#ring.middleware.stacktrace/wrap-stacktrace",
   :doc
   "Wrap a handler such that exceptions are caught, a corresponding stacktrace is\nlogged to *err*, and a helpful debugging web response is returned.",
   :var-type "function",
   :line 83,
   :file
   "/home/jim/Development/ring/./ring-devel/src/ring/middleware/stacktrace.clj"}
  {:arglists ([handler]),
   :name "wrap-stacktrace-log",
   :namespace "ring.middleware.stacktrace",
   :source-url
   "http://github.com/mmcgrana/ring/blob/38131596c5a2a686399542b6bf3cc931b47298e3/ring-devel/src/ring/middleware/stacktrace.clj#L10",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/38131596c5a2a686399542b6bf3cc931b47298e3/ring-devel/src/ring/middleware/stacktrace.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.stacktrace-api.html#ring.middleware.stacktrace/wrap-stacktrace-log",
   :doc
   "Wrap a handler such that exceptions are logged to *err* and then rethrown.",
   :var-type "function",
   :line 10,
   :file
   "/home/jim/Development/ring/./ring-devel/src/ring/middleware/stacktrace.clj"}
  {:arglists ([handler]),
   :name "wrap-stacktrace-web",
   :namespace "ring.middleware.stacktrace",
   :source-url
   "http://github.com/mmcgrana/ring/blob/38131596c5a2a686399542b6bf3cc931b47298e3/ring-devel/src/ring/middleware/stacktrace.clj#L73",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/38131596c5a2a686399542b6bf3cc931b47298e3/ring-devel/src/ring/middleware/stacktrace.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.stacktrace-api.html#ring.middleware.stacktrace/wrap-stacktrace-web",
   :doc
   "Wrap a handler such that exceptions are caught and a helpful debugging\nresponse is returned.",
   :var-type "function",
   :line 73,
   :file
   "/home/jim/Development/ring/./ring-devel/src/ring/middleware/stacktrace.clj"}
  {:arglists ([app public-dir statics]),
   :name "wrap-static",
   :namespace "ring.middleware.static",
   :source-url
   "http://github.com/mmcgrana/ring/blob/173cb2d2f1a8c7fa568bdc36fda9899fa3d89cb1/ring-core/src/ring/middleware/static.clj#L5",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/173cb2d2f1a8c7fa568bdc36fda9899fa3d89cb1/ring-core/src/ring/middleware/static.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.static-api.html#ring.middleware.static/wrap-static",
   :doc
   "Like ring.file, but takes an additional statics, a coll of Strings that will\nbe used to test incoming requests uris. If a uri begins with any of the\nstrings in the statics coll, the middleware will check to see if a file can be\nserved from the public-dir before proxying back to the given app; if the uri\ndoes not correspond to one of these strings, the middleware proxies the\nrequest directly back to the app without touching the filesystem.",
   :var-type "function",
   :line 5,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/static.clj"}
  {:arglists ([encoded]),
   :name "base64-decode",
   :namespace "ring.util.codec",
   :source-url
   "http://github.com/mmcgrana/ring/blob/ef089b9cce37079100bec71447d6bc860e67faf6/ring-core/src/ring/util/codec.clj#L26",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/ef089b9cce37079100bec71447d6bc860e67faf6/ring-core/src/ring/util/codec.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.codec-api.html#ring.util.codec/base64-decode",
   :doc "Decode a base64 encoded string into an array of bytes.",
   :var-type "function",
   :line 26,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/codec.clj"}
  {:arglists ([unencoded]),
   :name "base64-encode",
   :namespace "ring.util.codec",
   :source-url
   "http://github.com/mmcgrana/ring/blob/ef089b9cce37079100bec71447d6bc860e67faf6/ring-core/src/ring/util/codec.clj#L21",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/ef089b9cce37079100bec71447d6bc860e67faf6/ring-core/src/ring/util/codec.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.codec-api.html#ring.util.codec/base64-encode",
   :doc "Encode an array of bytes into a base64 encoded string.",
   :var-type "function",
   :line 21,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/codec.clj"}
  {:arglists ([encoded & [encoding]]),
   :name "url-decode",
   :namespace "ring.util.codec",
   :source-url
   "http://github.com/mmcgrana/ring/blob/ef089b9cce37079100bec71447d6bc860e67faf6/ring-core/src/ring/util/codec.clj#L13",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/ef089b9cce37079100bec71447d6bc860e67faf6/ring-core/src/ring/util/codec.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.codec-api.html#ring.util.codec/url-decode",
   :doc
   "Returns the form-url-decoded version of the given string, using either a\nspecified encoding or UTF-8 by default.",
   :var-type "function",
   :line 13,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/codec.clj"}
  {:arglists ([unencoded & [encoding]]),
   :name "url-encode",
   :namespace "ring.util.codec",
   :source-url
   "http://github.com/mmcgrana/ring/blob/ef089b9cce37079100bec71447d6bc860e67faf6/ring-core/src/ring/util/codec.clj#L7",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/ef089b9cce37079100bec71447d6bc860e67faf6/ring-core/src/ring/util/codec.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.codec-api.html#ring.util.codec/url-encode",
   :doc
   "Returns the form-url-encoded ersion of the given string, using either a\nspecified encoding or UTF-8 by default.",
   :var-type "function",
   :line 7,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/codec.clj"}
  {:arglists ([filename & [mime-types]]),
   :name "ext-mime-type",
   :namespace "ring.util.mime-type",
   :source-url
   "http://github.com/mmcgrana/ring/blob/b0f754f5159811b1766159a701afa26bde5a2a76/ring-core/src/ring/util/mime_type.clj#L92",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/b0f754f5159811b1766159a701afa26bde5a2a76/ring-core/src/ring/util/mime_type.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.mime-type-api.html#ring.util.mime-type/ext-mime-type",
   :doc
   "Get the mimetype from the filename extension. Takes an optional map of\nextensions to mimetypes that overrides values in the default-mime-types map.",
   :var-type "function",
   :line 92,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/mime_type.clj"}
  {:arglists ([resp content-type]),
   :name "content-type",
   :namespace "ring.util.response",
   :source-url
   "http://github.com/mmcgrana/ring/blob/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj#L90",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.response-api.html#ring.util.response/content-type",
   :doc
   "Returns an updated Ring response with the a Content-Type header corresponding\nto the given content-type.",
   :var-type "function",
   :line 90,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/response.clj"}
  {:arglists ([filepath & [opts]]),
   :name "file-response",
   :namespace "ring.util.response",
   :source-url
   "http://github.com/mmcgrana/ring/blob/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj#L55",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.response-api.html#ring.util.response/file-response",
   :doc
   "Returns a Ring response to serve a static file, or nil if an appropriate\nfile does not exist.\nOptions:\n  :root         - take the filepath relative to this root path\n  :index-files? - look for index.* files in directories, defaults to true",
   :var-type "function",
   :line 55,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/response.clj"}
  {:arglists ([resp name value]),
   :name "header",
   :namespace "ring.util.response",
   :source-url
   "http://github.com/mmcgrana/ring/blob/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj#L85",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.response-api.html#ring.util.response/header",
   :doc
   "Returns an updated Ring response with the specified header added.",
   :var-type "function",
   :line 85,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/response.clj"}
  {:arglists ([url]),
   :name "redirect",
   :namespace "ring.util.response",
   :source-url
   "http://github.com/mmcgrana/ring/blob/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj#L6",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.response-api.html#ring.util.response/redirect",
   :doc "Returns a Ring response for an HTTP 302 redirect.",
   :var-type "function",
   :line 6,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/response.clj"}
  {:arglists ([url]),
   :name "redirect-after-post",
   :namespace "ring.util.response",
   :source-url
   "http://github.com/mmcgrana/ring/blob/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj#L13",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.response-api.html#ring.util.response/redirect-after-post",
   :doc "Returns a Ring response for an HTTP 303 redirect.",
   :var-type "function",
   :line 13,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/response.clj"}
  {:arglists ([path & [opts]]),
   :name "resource-response",
   :namespace "ring.util.response",
   :source-url
   "http://github.com/mmcgrana/ring/blob/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj#L65",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.response-api.html#ring.util.response/resource-response",
   :doc
   "Returns a Ring response to serve a packaged resource, or nil if the\nresource does not exist.\nOptions:\n  :root - take the resource relative to this root",
   :var-type "function",
   :line 65,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/response.clj"}
  {:arglists ([body]),
   :name "response",
   :namespace "ring.util.response",
   :source-url
   "http://github.com/mmcgrana/ring/blob/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj#L20",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.response-api.html#ring.util.response/response",
   :doc
   "Returns a skeletal Ring response with the given body, status of 200, and no\nheaders.",
   :var-type "function",
   :line 20,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/response.clj"}
  {:arglists ([resp status]),
   :name "status",
   :namespace "ring.util.response",
   :source-url
   "http://github.com/mmcgrana/ring/blob/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj#L80",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/11a096e86bd2b9bc105b4dd032bd96265d17c317/ring-core/src/ring/util/response.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.response-api.html#ring.util.response/status",
   :doc "Returns an updated Ring response with the given status.",
   :var-type "function",
   :line 80,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/util/response.clj"}
  {:arglists ([request]),
   :name "build-request-map",
   :namespace "ring.util.servlet",
   :source-url
   "http://github.com/mmcgrana/ring/blob/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj#L29",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.servlet-api.html#ring.util.servlet/build-request-map",
   :doc "Create the request map from the HttpServletRequest object.",
   :var-type "function",
   :line 29,
   :file
   "/home/jim/Development/ring/./ring-servlet/src/ring/util/servlet.clj"}
  {:arglists ([handler] [prefix handler]),
   :name "defservice",
   :namespace "ring.util.servlet",
   :source-url
   "http://github.com/mmcgrana/ring/blob/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj#L153",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.servlet-api.html#ring.util.servlet/defservice",
   :doc
   "Defines a service method with an optional prefix suitable for being used by\ngenclass to compile a HttpServlet class.\ne.g. (defservice my-handler)\n     (defservice \"my-prefix-\" my-handler)",
   :var-type "macro",
   :line 153,
   :file
   "/home/jim/Development/ring/./ring-servlet/src/ring/util/servlet.clj"}
  {:arglists ([handler]),
   :name "make-service-method",
   :namespace "ring.util.servlet",
   :source-url
   "http://github.com/mmcgrana/ring/blob/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj#L130",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.servlet-api.html#ring.util.servlet/make-service-method",
   :doc
   "Turns a handler into a function that takes the same arguments and has the\nsame return value as the service method in the HttpServlet class.",
   :var-type "function",
   :line 130,
   :file
   "/home/jim/Development/ring/./ring-servlet/src/ring/util/servlet.clj"}
  {:arglists ([request-map servlet request response]),
   :name "merge-servlet-keys",
   :namespace "ring.util.servlet",
   :source-url
   "http://github.com/mmcgrana/ring/blob/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj#L45",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.servlet-api.html#ring.util.servlet/merge-servlet-keys",
   :doc
   "Associate servlet-specific keys with the request map for use with legacy\nsystems.",
   :var-type "function",
   :line 45,
   :file
   "/home/jim/Development/ring/./ring-servlet/src/ring/util/servlet.clj"}
  {:arglists ([handler]),
   :name "servlet",
   :namespace "ring.util.servlet",
   :source-url
   "http://github.com/mmcgrana/ring/blob/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj#L145",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.servlet-api.html#ring.util.servlet/servlet",
   :doc "Create a servlet from a Ring handler..",
   :var-type "function",
   :line 145,
   :file
   "/home/jim/Development/ring/./ring-servlet/src/ring/util/servlet.clj"}
  {:arglists ([response headers]),
   :name "set-headers",
   :namespace "ring.util.servlet",
   :source-url
   "http://github.com/mmcgrana/ring/blob/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj#L80",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.servlet-api.html#ring.util.servlet/set-headers",
   :doc "Update a HttpServletResponse with a map of headers.",
   :var-type "function",
   :line 80,
   :file
   "/home/jim/Development/ring/./ring-servlet/src/ring/util/servlet.clj"}
  {:arglists ([response status]),
   :name "set-status",
   :namespace "ring.util.servlet",
   :source-url
   "http://github.com/mmcgrana/ring/blob/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj#L58",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.servlet-api.html#ring.util.servlet/set-status",
   :doc "Update a HttpServletResponse with a status code.",
   :var-type "function",
   :line 58,
   :file
   "/home/jim/Development/ring/./ring-servlet/src/ring/util/servlet.clj"}
  {:arglists ([response {:keys [status headers body]}]),
   :name "update-servlet-response",
   :namespace "ring.util.servlet",
   :source-url
   "http://github.com/mmcgrana/ring/blob/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj#L119",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/2b3f560e9d9c15a8da0ec626f0f10bf9c5014ff4/ring-servlet/src/ring/util/servlet.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.util.servlet-api.html#ring.util.servlet/update-servlet-response",
   :doc "Update the HttpServletResponse using a response map.",
   :var-type "function",
   :line 119,
   :file
   "/home/jim/Development/ring/./ring-servlet/src/ring/util/servlet.clj"}
  {:arglists ([]),
   :name "byte-array-store",
   :namespace "ring.middleware.multipart-params.byte-array",
   :source-url
   "http://github.com/mmcgrana/ring/blob/d81d57358c62357e8dfc2e54660c811a3983737b/ring-core/src/ring/middleware/multipart_params/byte_array.clj#L4",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/d81d57358c62357e8dfc2e54660c811a3983737b/ring-core/src/ring/middleware/multipart_params/byte_array.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.multipart-params-api.html#ring.middleware.multipart-params.byte-array/byte-array-store",
   :doc
   "Returns a function that stores multipart file parameters as an array of\nbytes. The multipart parameters will be stored as maps with the following\nkeys:\n  :filename     - the name of the uploaded file\n  :content-type - the content type of the uploaded file\n  :bytes        - an array of bytes containing the uploaded content",
   :var-type "function",
   :line 4,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/multipart_params/byte_array.clj"}
  {:arglists ([] [{:keys [expires-in]}]),
   :name "temp-file-store",
   :namespace "ring.middleware.multipart-params.temp-file",
   :source-url
   "http://github.com/mmcgrana/ring/blob/d81d57358c62357e8dfc2e54660c811a3983737b/ring-core/src/ring/middleware/multipart_params/temp_file.clj#L29",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/d81d57358c62357e8dfc2e54660c811a3983737b/ring-core/src/ring/middleware/multipart_params/temp_file.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.multipart-params-api.html#ring.middleware.multipart-params.temp-file/temp-file-store",
   :doc
   "Returns a function that stores multipart file parameters as temporary files.\nAccepts the following options:\n  :expires-in - delete temporary files older than this many seconds\n                (defaults to 3600 - 1 hour)\nThe multipart parameters will be stored as maps with the following keys:\n  :filename     - the name of the uploaded file\n  :content-type - the content type of the upload file\n  :tempfile     - a File object that points to the temporary file containing\n                  the uploaded data.\n  :size         - the size in bytes of the uploaded data",
   :var-type "function",
   :line 29,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/multipart_params/temp_file.clj"}
  {:arglists ([] [options]),
   :name "cookie-store",
   :namespace "ring.middleware.session.cookie",
   :source-url
   "http://github.com/mmcgrana/ring/blob/0e68817324a79eb6bd460ee56b67a3a83efafc81/ring-core/src/ring/middleware/session/cookie.clj#L106",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/0e68817324a79eb6bd460ee56b67a3a83efafc81/ring-core/src/ring/middleware/session/cookie.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.session-api.html#ring.middleware.session.cookie/cookie-store",
   :doc "Creates an encrypted cookie storage engine.",
   :var-type "function",
   :line 106,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/session/cookie.clj"}
  {:arglists ([] [session-atom]),
   :name "memory-store",
   :namespace "ring.middleware.session.memory",
   :source-url
   "http://github.com/mmcgrana/ring/blob/4093a695fdfdf9531115b1b29b7009bf21f561e5/ring-core/src/ring/middleware/session/memory.clj#L18",
   :raw-source-url
   "http://github.com/mmcgrana/ring/raw/4093a695fdfdf9531115b1b29b7009bf21f561e5/ring-core/src/ring/middleware/session/memory.clj",
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.session-api.html#ring.middleware.session.memory/memory-store",
   :doc "Creates an in-memory session storage engine.",
   :var-type "function",
   :line 18,
   :file
   "/home/jim/Development/ring/./ring-core/src/ring/middleware/session/memory.clj"}
  {:raw-source-url nil,
   :source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.session-api.html#ring.middleware.session.store/delete-session",
   :namespace "ring.middleware.session.store",
   :var-type "function",
   :arglists ([store key]),
   :doc
   "Delete a session map from the store, and returns the session key. If the\nreturned key is nil, the session cookie will be removed.",
   :name "delete-session"}
  {:raw-source-url nil,
   :source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.session-api.html#ring.middleware.session.store/read-session",
   :namespace "ring.middleware.session.store",
   :var-type "function",
   :arglists ([store key]),
   :doc
   "Read a session map from the store. If the key is not found, an empty map\nis returned.",
   :name "read-session"}
  {:raw-source-url nil,
   :source-url nil,
   :wiki-url
   "http://mmcgrana.github.com/ring//ring.middleware.session-api.html#ring.middleware.session.store/write-session",
   :namespace "ring.middleware.session.store",
   :var-type "function",
   :arglists ([store key data]),
   :doc
   "Write a session map to the store. Returns the (possibly changed) key under\nwhich the data was stored. If the key is nil, the session is considered\nto be new, and a fresh key should be generated.",
   :name "write-session"})}

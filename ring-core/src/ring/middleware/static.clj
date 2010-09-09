(ns ring.middleware.static
  "Static file serving, more selective than ring.middleware.file."
  (:use ring.middleware.file))

(defn wrap-static
  "Like ring.file, but takes an additional statics, a coll of Strings that will
  be used to test incoming requests uris. If a uri begins with any of the
  strings in the statics coll, the middleware will check to see if a file can be
  served from the public-dir before proxying back to the given app; if the uri
  does not correspond to one of these strings, the middleware proxies the
  request directly back to the app without touching the filesystem."
  [app public-dir statics]
  (let [app-with-file (wrap-file app public-dir)]
    (fn [req]
      (let [^String uri (:uri req)]
        (if (some #(.startsWith uri %) statics)
          (app-with-file req)
          (app req))))))

(ns ring.static
  (:require ring.file)
  (:use ring.utils))

(defn wrap
  "Like ring.file, but takes an additional statics, a coll of Strings that will
  be used to test incoming requests uris. If the uri begins with any of the
  strings in statics, the middleware will check to see if a files can be served
  from the public-dir before proxying back to the given app; if the uri does not
  match the re, proxies the request directly back to the app without touching the
  filesystem."
  [public-dir statics app]
  (let [app-with-file (ring.file/wrap public-dir app)]
    (fn [req]
      (let [uri (:uri req)]
        (if (some #(.startsWith uri %) statics)
          (app-with-file req)
          (app req))))))
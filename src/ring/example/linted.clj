; An example of inserting the linter between each component to ensure 
; compliance to the Ring spec.

(ns ring.examples.linted
  (:use (ring.handler dump)
        (ring.middleware stacktrace file file-info reload lint)
        (ring.adapter jetty)))

(def app
  (-> handle-dump
    wrap-lint
    wrap-stacktrace
    wrap-lint
    wrap-file-info
    wrap-lint
    (wrap-file "src/ring/examples/public")
    wrap-lint
    (wrap-reload '(ring.dump)
    wrap-lint)))

(run-jetty app {:port 8080})
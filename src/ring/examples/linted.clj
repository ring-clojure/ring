; An example of inserting the linter inserted each component to ensure 
; compliance to the Ring spec.

(ns ring.examples.linted
  (:require (ring backtrace file file-info reload lint dump jetty))
  (:import (java.io File)))

(def app
  (ring.lint/wrap
    (ring.backtrace/wrap
      (ring.lint/wrap
        (ring.file-info/wrap
          (ring.lint/wrap
            (ring.file/wrap (File. "src/ring/examples/public")
              (ring.lint/wrap
                (ring.reload/wrap '(ring.dump)
                  ring.dump/app)))))))))

(ring.jetty/run {:port 8080} app)
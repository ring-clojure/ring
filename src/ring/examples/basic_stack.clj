; A failry typical middleware configuration wrapping the dump endpoint.

(ns ring.examples.basic-stack
  (:require (ring show-exceptions file-info file reloading dump jetty))
  (:import (java.io File)))

(def app
  (ring.show-exceptions/wrap
    (ring.file-info/wrap
      (ring.file/wrap (File. "src/ring/examples/public")
        (ring.reloading/wrap '(ring.dump)
          ring.dump/app)))))

(ring.jetty/run {:port 8080} app)

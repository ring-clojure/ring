; A example of modular construction of Ring apps.

(ns ring.examples.wrapping
  (:require (ring show-exceptions file-info file reloading dump jetty))
  (:import (java.io File)))

(def app
  (ring.show-exceptions/wrap
    (ring.file-info/wrap
      (ring.file/wrap (File. "src/ring/examples/public")
        (fn [req]
          (if (= "/error" (:uri req))
            (throw (Exception. "Demonstrating ring.show-exceptions"))
            (ring.dump/app req)))))))

(ring.jetty/run {:port 8080} app)

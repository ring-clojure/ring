; A example of modular construction of Ring apps.

(ns ring.examples.wrapping
  (:require (ring backtrace file-info file dump jetty))
  (:import (java.io File)))

(def app
  (ring.backtrace/wrap
    (ring.file-info/wrap
      (ring.file/wrap (File. "src/ring/examples/public")
        (fn [req]
          (if (= "/error" (:uri req))
            (throw (Exception. "Demonstrating ring.backtrace"))
            (ring.dump/app req)))))))

(ring.jetty/run {:port 8080} app)

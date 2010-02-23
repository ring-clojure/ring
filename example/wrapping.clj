; A example of modular construction of Ring apps.

(ns ring.example.wrapping
  (:use (ring.handler dump)
        (ring.middleware stacktrace file-info file)
        (ring.adapter jetty)
        (clojure.contrib except)))

(defn wrap-error [app]
  (fn [req]
    (if (= "/error" (:uri req))
      (throwf "Demonstrating ring.middleware.stacktrace")
      (app req))))

(def app
  (-> handle-dump
    wrap-error
    (wrap-file "example/public")
    wrap-file-info
    wrap-stacktrace))

(run-jetty app {:port 8080})

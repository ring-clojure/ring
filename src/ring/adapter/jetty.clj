(ns ring.adapter.jetty
  (:import (org.mortbay.jetty.handler AbstractHandler)
           (org.mortbay.jetty Server))
  (:use (ring.util servlet)
        (clojure.contrib except)))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  (proxy [AbstractHandler] []
    (handle [target request response dispatch]
      (let [request-map  (build-request-map request)
            response-map (handler request-map)]
        (update-servlet-response response response-map)
        (.setHandled request true)))))

(defn run-jetty
  "Serve the given handler according to the options.
  Options:
    :port, an Integer."
  [handler options]
  (let [port    (or (:port options) (throwf ":port missing from options"))
        server  (doto (Server. port) (.setSendDateHeader true))
        handler (proxy-handler handler)]
    (.setHandler server handler)
    (.start server)
    (.join  server)))

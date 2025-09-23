(ns ring.sse
  "Protocols and utility functions for SSE support."
  (:refer-clojure :exclude [send])
  (:require [ring.sse.protocols :as p]))


(extend-type clojure.lang.IPersistentMap
  p/Listener
  (on-open [m sender]
    (when-let [kv (find m :on-open)] ((val kv) sender))))


(defn send
  "Sends a SSE message"
  [sender sso-message]
  (p/-send sender sso-message))


(defn sse-response?
  "Returns true if the response contains a SSE emitter."
  [response]
  (contains? response ::listener))


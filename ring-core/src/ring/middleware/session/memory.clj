(ns ring.middleware.session.memory
  "A session storage engine that stores session data in memory."
  (:require [ring.middleware.session.store :refer [SessionStore]])
  (:import [java.util UUID]))

(deftype MemoryStore [session-map]
  SessionStore
  (read-session [_ key]
    (@session-map key))
  (write-session [_ key data]
    (let [key (or key (str (UUID/randomUUID)))]
      (swap! session-map assoc key data)
      key))
  (delete-session [_ key]
    (swap! session-map dissoc key)
    nil))

(ns-unmap *ns* '->MemoryStore)

(defn memory-store
  "Creates an in-memory session storage engine. Accepts an atom as an optional
  argument; if supplied, the atom is used to hold the session data."
  ([] (memory-store (atom {})))
  ([session-atom] (MemoryStore. session-atom)))

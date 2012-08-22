(ns ring.middleware.session.memory
  "In-memory session storage."
  (:use ring.middleware.session.store)
  (:import java.util.UUID))

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

(defn memory-store
  "Creates an in-memory session storage engine."
  ([] (memory-store (atom {})))
  ([session-atom] (MemoryStore. session-atom)))

(ns ring.middleware.session.memory
  "In-memory session storage."
  (:import java.util.UUID))

(defn memory-store
  "Creates an in-memory session storage engine."
  []
  (let [session-map (atom {})]
    {:read (fn [session-key]
             (@session-map session-key {}))
     :write (fn [session-key session]
              (let [session-key (or session-key (str (UUID/randomUUID)))]
                (swap! session-map assoc session-key session)
                session-key))
     :delete (fn [session-key]
               (swap! session-map dissoc session-key)
               nil)}))

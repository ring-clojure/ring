(ns ring.middleware.session.store
  "Common session store objects and functions.")

(defprotocol SessionStore
  (read-session [store key] "Read a session map from the store.")
  (write-session [store key data] "Write a session map to the store.")
  (delete-session [store key] "Delete a session map from the store."))

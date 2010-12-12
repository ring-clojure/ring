(ns ring.middleware.session.store
  "Common session store objects and functions.")

(defprotocol SessionStore
  (read-session [store key]
    "Read a session map from the store. If the key is not found, an empty map
    is returned.")
  (write-session [store key data]
    "Write a session map to the store. Returns the (possibly changed) key under
    which the data was stored. If the key is nil, the session is considered
    to be new, and a fresh key should be generated.")
  (delete-session [store key]
    "Delete a session map from the store, and returns the session key. If the
    returned key is nil, the session cookie will be removed."))

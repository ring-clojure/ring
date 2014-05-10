(ns ring.middleware.session.store
  "Contains the protocol used to define all Ring session storage engines.")

(defprotocol SessionStore
  "An interface to a session storage engine. Implementing this protocol allows
  Ring session data to be stored in different places."
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

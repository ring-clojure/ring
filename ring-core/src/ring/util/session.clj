(ns ring.util.session
  (:require [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.session.store :as st]))

(defn read-key
  "Reads at most 16 bytes from the given path"
  [key-path]
  (with-open [is (java.io.FileInputStream. key-path)]
    (let [bytes (byte-array 16)]
      (.read is bytes)
      bytes)))

(defprotocol Rekeyable
  (rekey [this]))

(defn rekeyable-cookie-store
  "Returns a cookie-store that supports reloading secret key.
  Takes a path to a file containg the key.
  The key can be reread by invoking the rekey method.

  A usage example might be to pass a reference to this cookie-store to
  a handler, to which access is allowed only from localhost, which can
  then call the rekey method. This allows to rotate the secret key
  without server restart."
  [key-path]
  (let [store (atom (cookie-store {:key (read-key key-path)}))]
    (reify
      st/SessionStore
      (read-session [_ key]
        (st/read-session @store key))
      (write-session [_ key data]
        (st/write-session @store key data))
      (delete-session [_ key]
        (st/delete-session @store key))
      Rekeyable
      (rekey [_]
        (reset! store (cookie-store {:key (read-key key-path)}))))))

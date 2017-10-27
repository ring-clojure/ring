(ns ring.middleware.session.cookie
  "A session storage engine that stores session data in encrypted cookies."
  (:require [ring.middleware.session.store :refer [SessionStore]]
            [ring.util.codec :as codec]
            [clojure.edn :as edn]
            [crypto.random :as random]
            [crypto.equality :as crypto])
  (:import [java.security SecureRandom]
           [javax.crypto Cipher Mac]
           [javax.crypto.spec SecretKeySpec IvParameterSpec]))

(def ^{:private true
       :doc "Algorithm to generate a HMAC."}
  hmac-algorithm
  "HmacSHA256")

(def ^{:private true
       :doc "Type of encryption to use."}
  crypt-type
  "AES")

(def ^{:private true
       :doc "Full algorithm to encrypt data with."}
  crypt-algorithm
  "AES/CBC/PKCS5Padding")

;; Ensure cipher-algorithm classes are preloaded
(Cipher/getInstance crypt-algorithm)

(defn- hmac
  "Generates a Base64 HMAC with the supplied key on a string of data."
  [key data]
  (let [mac (Mac/getInstance hmac-algorithm)]
    (.init mac (SecretKeySpec. key hmac-algorithm))
    (codec/base64-encode (.doFinal mac data))))

(defn- encrypt
  "Encrypt a string with a key."
  [key data]
  (let [cipher     (Cipher/getInstance crypt-algorithm)
        secret-key (SecretKeySpec. key crypt-type)
        iv         (random/bytes (.getBlockSize cipher))]
    (.init cipher Cipher/ENCRYPT_MODE secret-key (IvParameterSpec. iv))
    (->> (.doFinal cipher data)
      (concat iv)
      (byte-array))))

(defn- decrypt
  "Decrypt an array of bytes with a key."
  [key data]
  (let [cipher     (Cipher/getInstance crypt-algorithm)
        secret-key (SecretKeySpec. key crypt-type)
        [iv data]  (split-at (.getBlockSize cipher) data)
        iv-spec    (IvParameterSpec. (byte-array iv))]
    (.init cipher Cipher/DECRYPT_MODE secret-key iv-spec)
    (String. (.doFinal cipher (byte-array data)))))

(defn- get-secret-key
  "Get a valid secret key from a map of options, or create a random one from
  scratch."
  [options]
  (if-let [secret-key (:key options)]
    (if (string? secret-key)
      (.getBytes ^String secret-key)
      secret-key)
    (random/bytes 16)))

(defn- deserialize [x options]
  (edn/read-string (select-keys options [:readers]) x))

(defn- ^String serialize [x options]
  {:post [(= x (deserialize % options))]}
  (pr-str x))

(defn- seal
  "Seal a Clojure data structure into an encrypted and HMACed string."
  [key data options]
  (let [data (encrypt key (.getBytes (serialize data options)))]
    (str (codec/base64-encode data) "--" (hmac key data))))

(defn- unseal
  "Retrieve a sealed Clojure data structure from a string"
  [key ^String string options]
  (let [[data mac] (.split string "--")
        data (codec/base64-decode data)]
    (if (crypto/eq? mac (hmac key data))
      (deserialize (decrypt key data) options))))

(deftype CookieStore [secret-key options]
  SessionStore
  (read-session [_ data]
    (if data (unseal secret-key data options)))
  (write-session [_ _ data]
    (seal secret-key data options))
  (delete-session [_ _]
    (seal secret-key {} options)))

(ns-unmap *ns* '->CookieStore)

(defn- valid-secret-key? [key]
  (and (= (type (byte-array 0)) (type key))
       (= (count key) 16)))

(defn cookie-store
  "Creates an encrypted cookie storage engine. Accepts the following options:

  :key - The secret key to encrypt the session cookie. Must be exactly 16 bytes
         If no key is provided then a random key will be generated. Note that in
         that case a server restart will invalidate all existing session
         cookies.

  :readers - A map of data readers used to read the serialized edn from the
             cookie. For writing, ensure that each data type has a key in the
             clojure.core/print-method or clojure.core/print-dup multimethods."
  ([] (cookie-store {}))
  ([options]
    (let [key (get-secret-key options)]
      (assert (valid-secret-key? key) "the secret key must be exactly 16 bytes")
      (CookieStore. key options))))

(ns ring.middleware.session.cookie
  "A session storage engine that stores session data in encrypted cookies."
  (:use ring.middleware.session.store)
  (:require [ring.util.codec :as codec]
            [clojure.tools.reader.edn :as edn]
            [crypto.random :as random]
            [crypto.equality :as crypto])
  (:import java.security.SecureRandom
           (javax.crypto Cipher Mac)
           (javax.crypto.spec SecretKeySpec IvParameterSpec)))

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

(defn- ^String serialize [x]
  {:post [(= x (edn/read-string %))]}
  (pr-str x))

(defn- seal
  "Seal a Clojure data structure into an encrypted and HMACed string."
  [key data]
  (let [data (encrypt key (.getBytes (serialize data)))]
    (str (codec/base64-encode data) "--" (hmac key data))))

(defn- unseal
  "Retrieve a sealed Clojure data structure from a string"
  [key ^String string]
  (let [[data mac] (.split string "--")
        data (codec/base64-decode data)]
    (if (crypto/eq? mac (hmac key data))
      (edn/read-string (decrypt key data)))))

(deftype CookieStore [secret-key]
  SessionStore
  (read-session [_ data]
    (if data (unseal secret-key data)))
  (write-session [_ _ data]
    (seal secret-key data))
  (delete-session [_ _]
    (seal secret-key {})))

(ns-unmap *ns* '->CookieStore)

(defn cookie-store
  "Creates an encrypted cookie storage engine. Accepts the following options:

  :key - The secret key to encrypt the session cookie. Must be exactly 16 bytes
         If no key is provided then a random key will be generated. Note that in
         that case a server restart will invalidate all existing session
         cookies."
  ([] (cookie-store {}))
  ([options]
    (CookieStore. (get-secret-key options))))

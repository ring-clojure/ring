(ns ring.middleware.session.cookie
  "Encrypted cookie session storage."
  (:use ring.middleware.session.store)
  (:require [ring.util.codec :as codec])
  (:import java.security.SecureRandom
           (javax.crypto Cipher Mac)
           (javax.crypto.spec SecretKeySpec IvParameterSpec)))

(def ^{:private true
       :doc "Algorithm to seed random numbers."}
  seed-algorithm
  "SHA1PRNG")

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

(defn- secure-random-bytes
  "Returns a random byte array of the specified size."
  [size]
  (let [seed (byte-array size)]
    (.nextBytes (SecureRandom/getInstance seed-algorithm) seed)
    seed))

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
        iv         (secure-random-bytes (.getBlockSize cipher))]
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
    (secure-random-bytes 16)))

(defn- seal
  "Seal a Clojure data structure into an encrypted and HMACed string."
  [key data]
  (let [data (encrypt key (.getBytes (pr-str data)))]
    (str (codec/base64-encode data) "--" (hmac key data))))

(defn- secure-compare [a b]
  (if (and a b (= (.length a) (.length b)))
      (= 0
         (reduce bit-or
                 (map bit-xor
                      (.getBytes a)
                      (.getBytes b))))
      false))

(defn- unseal
  "Retrieve a sealed Clojure data structure from a string"
  [key ^String string]
  (let [[data mac] (.split string "--")
        data (codec/base64-decode data)]
    (if (secure-compare mac (hmac key data))
        (read-string (decrypt key data)))))

(deftype CookieStore [secret-key]
  SessionStore
  (read-session [_ data]
    (or (if data (unseal secret-key data)) {}))
  (write-session [_ _ data]
    (seal secret-key data))
  (delete-session [_ _]
    (seal secret-key {})))

(defn cookie-store
  "Creates an encrypted cookie storage engine."
  ([] (cookie-store {}))
  ([options]
    (CookieStore. (get-secret-key options))))

(ns ring.middleware.session.cookie
  "Encrypted cookie session storage."
  (:use clojure.contrib.def)
  (:use clojure.contrib.base64)
  (:import java.security.SecureRandom
           [javax.crypto Cipher Mac]
           [javax.crypto.spec SecretKeySpec IvParameterSpec]
           org.apache.commons.codec.binary.Base64))

(defvar- seed-algorithm "SHA1PRNG"
  "Algorithm to seed random numbers.")

(defvar- hmac-algorithm "HmacSHA256"
  "Algorithm to generate a HMAC.")

(defvar- crypt-type "AES"
  "Type of encryption to use.")

(defvar- crypt-algorithm "AES/CBC/PKCS5Padding"
  "Full algorithm to encrypt data with.")

(defn- secure-random-bytes
  "Returns a random byte array of the specified size."
  [size]
  (let [seed (byte-array size)]
    (.nextBytes (SecureRandom/getInstance seed-algorithm) seed)
    seed))

(defn- base64-encode
  "Encode an array of bytes into a base64 encoded string."
  [unencoded]
  (String. (Base64/encodeBase64 unencoded)))

(defn- base64-decode
  "Decode a base64 encoded string into an array of bytes."
  [encoded]
  (Base64/decodeBase64 (.getBytes encoded)))

(defn- hmac
  "Generates a Base64 HMAC with the supplied key on a string of data."
  [key data]
  (let [mac (Mac/getInstance hmac-algorithm)]
    (.init mac (SecretKeySpec. key hmac-algorithm))
    (base64-encode (.doFinal mac data))))

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
      (.getBytes secret-key)
      secret-key)
    (secure-random-bytes 16)))

(defn- seal
  "Seal a Clojure data structure into an encrypted and HMACed string."
  [key data]
  (let [data (encrypt key (.getBytes (pr-str data)))]
    (str (base64-encode data) "--" (hmac key data))))

(defn- unseal
  "Retrieve a sealed Clojure data structure from a string"
  [key string]
  (let [[data mac] (.split string "--")
        data (base64-decode data)]
    (if (= mac (hmac key data))
      (read-string (decrypt key data)))))

(defn cookie-store
  "Creates an encrypted cookie storage engine."
  ([]
    (cookie-store {}))
  ([options]
    (let [secret-key (get-secret-key options)]
      {:read (fn [session-data]
               (if session-data
                 (or (unseal secret-key session-data) {})
                 {}))
       :write (fn [_ session]
                (seal secret-key session))
       :delete (fn [_]
                 (seal secret-key {}))})))

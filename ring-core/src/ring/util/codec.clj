(ns ring.util.codec
  "Encoding and decoding utilities."
  (:use ring.util.data)
  (:require [clojure.string :as str])
  (:import java.io.File
           java.util.Map
           (java.net URLEncoder URLDecoder)
           org.apache.commons.codec.binary.Base64))

(defn- double-escape [^String x]
  (.replace x "\\" "\\\\"))

(defn percent-encode
  "Percent-encode every character in the given string using either the specified
  encoding, or UTF-8 by default."
  [unencoded & [encoding]]
  (->> (.getBytes unencoded (or encoding "UTF-8"))
       (map (partial format "%%%02X"))
       (str/join)))

(defn- parse-bytes [encoded-bytes]
  (->> (re-seq #"%.." encoded-bytes)
       (map #(subs % 1))
       (map #(.byteValue (Integer/parseInt % 16)))
       (byte-array)))

(defn percent-decode
  "Decode every percent-encoded character in the given string using the
  specified encoding, or UTF-8 by default."
  [encoded & [encoding]]
  (str/replace encoded
               #"(?:%..)+"
               (fn [chars]
                 (-> (parse-bytes chars)
                     (String. (or encoding "UTF-8"))
                     (double-escape)))))

(defn url-encode
  "Returns the url-encoded version of the given string, using either a specified
  encoding or UTF-8 by default."
  [unencoded & [encoding]]
  (str/replace
    unencoded
    #"[^A-Za-z0-9_~.+-]+"
    #(double-escape (percent-encode % encoding))))

(defn url-decode
  "Returns the url-decoded version of the given string, using either a specified
  encoding or UTF-8 by default. If the encoding is invalid, nil is returned."
  [encoded & [encoding]]
  (percent-decode encoded encoding))

(defn base64-encode
  "Encode an array of bytes into a base64 encoded string."
  [unencoded]
  (String. (Base64/encodeBase64 unencoded)))

(defn base64-decode
  "Decode a base64 encoded string into an array of bytes."
  [^String encoded]
  (Base64/decodeBase64 (.getBytes encoded)))

(defprotocol FormEncodeable
  (form-encode* [x encoding]))

(defn form-encode
  "Encode the supplied value into www-form-urlencoded format, often used in
  URL query strings and POST request bodies, using the specified encoding.
  If the encoding is not specified, it defaults to UTF-8"
  [x & [encoding]]
  (form-encode* x (or encoding "UTF-8")))

(extend-protocol FormEncodeable
  String
  (form-encode* [unencoded encoding]
    (URLEncoder/encode unencoded encoding))
  Map
  (form-encode* [params encoding]
    (letfn [(encode [x] (form-encode x encoding))
            (encode-param [[k v]] (str (encode (name k)) "=" (encode v)))]
      (->> params
           (mapcat
            (fn [[k v]]
              (if (or (seq? v) (sequential? v) )
                (map #(encode-param [k %]) v)
                [(encode-param [k v])])))
           (str/join "&"))))
  Object
  (form-encode* [x encoding]
    (form-encode* (str x) encoding)))

(defn form-decode
  "Decode the supplied www-form-urlencoded string using the specified encoding,
  or UTF-8 by default. If the encoded value is a string, a string is returned.
  If the encoded value is a map of parameters, a map is returned."
  [^String encoded & [encoding]]
  (letfn [(decode [s] (URLDecoder/decode s (or encoding "UTF-8")))]
    (if-not (.contains encoded "=")
      (decode encoded)
      (reduce
       (fn [m param]
         (if-let [[k v] (str/split param #"=" 2)]
           (assoc+ m (decode k) (decode v))
           m))
       {}
       (str/split encoded #"&")))))

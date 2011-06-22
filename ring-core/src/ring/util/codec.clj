(ns ring.util.codec
  "Encoding and decoding utilities."
  (:import java.io.File
           (java.net URLEncoder URLDecoder)
           org.apache.commons.codec.binary.Base64))

(defn url-encode
  "Returns the form-url-encoded ersion of the given string, using either a
  specified encoding or UTF-8 by default."
  [unencoded & [encoding]]
  (URLEncoder/encode unencoded (or encoding "UTF-8")))

(defn url-decode
  "Returns the form-url-decoded version of the given string, using either a
  specified encoding or UTF-8 by default."
  [encoded & [encoding]]
  (try
    (URLDecoder/decode encoded (or encoding "UTF-8"))
    (catch Exception e nil)))

(defn base64-encode
  "Encode an array of bytes into a base64 encoded string."
  [unencoded]
  (String. (Base64/encodeBase64 unencoded)))

(defn base64-decode
  "Decode a base64 encoded string into an array of bytes."
  [^String encoded]
  (Base64/decodeBase64 (.getBytes encoded)))

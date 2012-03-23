(ns ring.util.codec
  "Encoding and decoding utilities."
  (:use ring.util.data)
  (:require [clojure.string :as string])
  (:import java.io.File
           (java.net URLEncoder URLDecoder)
           org.apache.commons.codec.binary.Base64))

(defn url-encode
  "Returns the url-encoded version of the given string, using either a specified
  encoding or UTF-8 by default."
  [unencoded & [encoding]]
  (URLEncoder/encode unencoded (or encoding "UTF-8")))

(defn url-decode
  "Returns the url-decoded version of the given string, using either a specified
  encoding or UTF-8 by default. If the encoding is invalid, nil is returned."
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

(defn form-decode
  "Parse parameters from a string into a map."
  ([^String param-string]
     (form-decode param-string "UTF-8"))
  ([^String param-string encoding]
     (reduce
      (fn [param-map encoded-param]
        (if-let [[_ key val] (re-matches #"([^=]+)=(.*)" encoded-param)]
          (assoc+ param-map
                  (url-decode key encoding)
                  (url-decode (or val "") encoding))
          param-map))
      {}
      (string/split param-string #"&"))))

(defn form-encode
  "Encode parameters from a map into a string."
  ([param-map]
     (form-encode param-map "UTF-8"))
  ([param-map encoding]
     (form-encode (keys param-map)
                  (vals param-map)
                  encoding))
  ([params values encoding]
     (string/join #"&"
                  (map (fn [param value]
                         (if (vector? value)
                           (form-encode (repeat (count value) param)
                                        value
                                        encoding)
                           (str (url-encode param)
                                "="
                                (url-encode value))))
                       params values))))

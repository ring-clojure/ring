(ns ring.utils
  (:use clojure.contrib.str-utils))

(defn url-decode
  "Returns the form-url-decoded version of the given string."
  [encoded]
  (java.net.URLDecoder/decode encoded "UTF-8"))

(defn str-includes?
  "Returns logical truth iff the given target appears in the given string"
  [target string]
  (<= 0 (.indexOf string target)))

(defn re-match?
  "Returns true iff the given string contains a match for the given pattern."
  [#^java.util.regex.Pattern pattern string]
  (.find (.matcher pattern string)))

(defn re-get
  "Returns the nth captured group resulting from matching the given pattern
  against the given string, or nil if no match is found."
  [re n s]
  (let [m (re-matcher re s)]
    (if (.find m)
      (.group m n))))
(ns ring.util.parsing
  "Regular expressions for parsing HTTP.

  For internal use.")

(def ^{:doc "HTTP token: 1*<any CHAR except CTLs or tspecials>. See RFC2068"
       :added "1.3"}
  re-token
  #"[!#$%&'*\-+.0-9A-Z\^_`a-z\|~]+")

(def ^{:doc "HTTP quoted-string: <\"> *<any TEXT except \"> <\">. See RFC2068."
       :added "1.3"}
  re-quoted
  #"\"((?:\\\"|[^\"])*)\"")

(def ^{:doc "HTTP value: token | quoted-string. See RFC2109"
       :added "1.3"}
  re-value
  (str "(" re-token ")|" re-quoted))

(def ^{:doc "Pattern for pulling the charset out of the content-type header"
       :added "1.6"}
  re-charset
  (re-pattern (str ";(?:.*\\s)?(?i:charset)=(?:" re-value ")\\s*(?:;|$)")))

(defn find-content-type-charset
  "Return the charset of a given a content-type string."
  {:added "1.8.1"}
  [s]
  (when-let [m (re-find re-charset s)]
    (or (m 1) (m 2))))

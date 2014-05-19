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
  #"\"(\\\"|[^\"])*\"")

(def ^{:doc "HTTP value: token | quoted-string. See RFC2109"
       :added "1.3"}
  re-value
  (str re-token "|" re-quoted))

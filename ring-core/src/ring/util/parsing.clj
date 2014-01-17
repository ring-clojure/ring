(ns ring.util.parsing
  "Regular expressions for parsing HTTP.")

(def ^{:doc "HTTP token: 1*<any CHAR except CTLs or tspecials>. See RFC2068"}
  re-token
  #"[!#$%&'*\-+.0-9A-Z\^_`a-z\|~]+")

(def ^{:doc "HTTP quoted-string: <\"> *<any TEXT except \"> <\">. See RFC2068."}
  re-quoted
  #"\"(\\\"|[^\"])*\"")

(def ^{:doc "HTTP value: token | quoted-string. See RFC2109"}
  re-value
  (str re-token "|" re-quoted))

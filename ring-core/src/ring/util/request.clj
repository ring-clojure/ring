(ns ring.util.request
  "Devive information from a request map.")

(defn request-url
  "Return the full URL of the request."
  [request]
  (str (-> request :scheme name)
       "://"
       (get-in request [:headers "host"])
       (:uri request)
       (if-let [query (:query-string request)]
         (str "?" query))))

(defprotocol WithRequestBody
  (body-string [request]))

(extend-protocol WithRequestBody
  nil
  (body-string [self] nil)

  clojure.lang.IPersistentMap
  (body-string [self]
    (body-string (:body self)))

  String
  (body-string [self]
    self)

  clojure.lang.ISeq
  (body-string [self]
    (apply str self))

  java.io.File
  (body-string [self]
    (slurp self))

  java.io.InputStream
  (body-string [self]
    (slurp self)))

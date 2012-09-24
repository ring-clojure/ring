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

(defmulti body-string
  "Return the request body as a string."
  (comp class :body))

(defmethod body-string nil [_] nil)

(defmethod body-string String [request]
  (:body request))

(defmethod body-string clojure.lang.ISeq [request]
  (apply str (:body request)))

(defmethod body-string java.io.File [request]
  (slurp (:body request)))

(defmethod body-string java.io.InputStream [request]
  (slurp (:body request)))

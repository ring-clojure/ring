(ns ring.util.request
  "Functions for augmenting and pulling information from request maps."
  (:use [ring.util.parsing :only (re-value)]))

(defn request-url
  "Return the full URL of the request."
  [request]
  (str (-> request :scheme name)
       "://"
       (get-in request [:headers "host"])
       (:uri request)
       (if-let [query (:query-string request)]
         (str "?" query))))

(defn content-type
  "Return the content-type of the request, or nil if no content-type is set."
  [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (second (re-find #"^(.*?)(?:;|$)" type))))

(defn content-length
  "Return the content-length of the request, or nil no content-length is set."
  [request]
  (if-let [length (get-in request [:headers "content-length"])]
    (Long. length)))

(def ^:private charset-pattern
  (re-pattern (str ";(?:.*\\s)?(?i:charset)=(" re-value ")\\s*(?:;|$)")))

(defn character-encoding
  "Return the character encoding for the request, or nil if it is not set."
  [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (second (re-find charset-pattern type))))

(defmulti body-string
  "Return the request body as a string."
  {:arglists '([request])}
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

(defn path-info
  "Returns the relative path of the request."
  [request]
  (or (:path-info request)
      (:uri request)))

(defn in-context?
  "Returns true if the URI of the request is a subpath of the supplied context."
  [request context]
  (.startsWith ^String (:uri request) context))

(defn set-context
  "Associate a context and path-info with the  request. The request URI must be
  a subpath of the supplied context."
  [request ^String context]
  {:pre [(in-context? request context)]}
  (assoc request
    :context context
    :path-info (subs (:uri request) (.length context))))

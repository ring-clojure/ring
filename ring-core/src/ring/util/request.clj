(ns ring.util.request
  "Functions for augmenting and pulling information from request maps."
  (:require [ring.util.parsing :refer [re-value]]))

(defn request-url
  "Return the full URL of the request."
  {:added "1.2"}
  [request]
  (str (-> request :scheme name)
       "://"
       (get-in request [:headers "host"])
       (:uri request)
       (if-let [query (:query-string request)]
         (str "?" query))))

(defn content-type
  "Return the content-type of the request, or nil if no content-type is set."
  {:added "1.3"}
  [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (second (re-find #"^(.*?)(?:;|$)" type))))

(defn content-length
  "Return the content-length of the request, or nil no content-length is set."
  {:added "1.3"}
  [request]
  (if-let [^String length (get-in request [:headers "content-length"])]
    (Long. length)))

(def ^:private charset-pattern
  (re-pattern (str ";(?:.*\\s)?(?i:charset)=(" re-value ")\\s*(?:;|$)")))

(defn character-encoding
  "Return the character encoding for the request, or nil if it is not set."
  {:added "1.3"}
  [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (second (re-find charset-pattern type))))

(defn urlencoded-form?
  "True if a request contains a urlencoded form in the body."
  {:added "1.3"}
  [request]
  (if-let [^String type (content-type request)]
    (.startsWith type "application/x-www-form-urlencoded")))

(defmulti ^String body-string
  "Return the request body as a string."
  {:arglists '([request]), :added "1.2"}
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
  {:added "1.2"}
  [request]
  (or (:path-info request)
      (:uri request)))

(defn in-context?
  "Returns true if the URI of the request is a subpath of the supplied context."
  {:added "1.2"}
  [request context]
  (.startsWith ^String (:uri request) context))

(defn set-context
  "Associate a context and path-info with the  request. The request URI must be
  a subpath of the supplied context."
  {:added "1.2"}
  [request ^String context]
  {:pre [(in-context? request context)]}
  (assoc request
    :context context
    :path-info (subs (:uri request) (.length context))))

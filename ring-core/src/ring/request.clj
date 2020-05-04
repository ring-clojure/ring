(ns ring.request
  "Core protocols and functions for Ring 2 request maps."
  {:added "2.0"}
  (:require [ring.util.parsing :as parsing]))

(defprotocol Request
  "A protocol representing a HTTP request. By default satisfied by maps that
  use either Ring 1 or Ring 2 keys. May be extended to provide performant
  access to underlying request objects without the need for a map lookup."
  (server-port     [req])
  (server-name     [req])
  (remote-addr     [req])
  (ssl-client-cert [req])
  (method          [req])
  (scheme          [req])
  (path            [req])
  (query           [req])
  (protocol        [req])
  (headers         [req])
  (body            [req])
  (get-header [req name]))

(defprotocol StreamableRequestBody
  "A protocol for reading the request body as an input stream."
  (-body-stream [body request]))

(defn ^java.io.InputStream body-stream
  "Given a request map, return an input stream to read the body."
  [request]
  (-body-stream (body request) request))

(defn charset
  "Given a request map, return the charset of the content-type header."
  [request]
  (when-let [content-type (get-header request "content-type")]
    (second (re-find parsing/re-charset content-type))))

(extend-protocol Request
  clojure.lang.IPersistentMap
  (server-port     [req] (::server-port     req (:server-port req)))
  (server-name     [req] (::server-name     req (:server-name req)))
  (remote-addr     [req] (::remote-addr     req (:remote-addr req)))
  (ssl-client-cert [req] (::ssl-client-cert req (:ssl-client-cert req)))
  (method          [req] (::method          req (:request-method req)))
  (scheme          [req] (::scheme          req (:scheme req)))
  (path            [req] (::path            req (:uri req)))
  (query           [req] (::query           req (:query-string req)))
  (protocol        [req] (::protocol        req (:protocol req)))
  (headers         [req] (::headers         req (:headers req)))
  (body            [req] (::body            req (:body req)))
  (get-header [req name] (get (headers req) name)))

(extend-protocol StreamableRequestBody
  (Class/forName "[B")
  (-body-stream [bs _] (java.io.ByteArrayInputStream. ^bytes bs))
  java.io.InputStream
  (-body-stream [stream _] stream)
  String
  (-body-stream [^String s request]
    (java.io.ByteArrayInputStream.
     (if-let [encoding (charset request)]
       (.getBytes s encoding)
       (.getBytes s))))
  nil
  (-body-stream [_ _] nil))

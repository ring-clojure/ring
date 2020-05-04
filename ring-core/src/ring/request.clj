(ns ring.request
  "Core protocols and functions for Ring 2 request maps."
  {:added "2.0"}
  (:require [ring.util.parsing :as parsing]))

(defprotocol StreamableRequestBody
  "A protocol for reading the request body as an input stream."
  (-body-stream [body request]))

(defn ^java.io.InputStream body-stream
  "Given a Ring 2 request map, return an input stream to read the body."
  [request]
  (-body-stream (::body request) request))

(defn charset
  "Given a request map, return the charset of the content-type header."
  [request]
  (when-let [content-type (get-header request "content-type")]
    (second (re-find parsing/re-charset content-type))))

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

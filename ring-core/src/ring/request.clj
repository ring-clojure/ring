(ns ring.request
  "Essential protocols for Ring 2 request maps."
  {:added "2.0"})

(defprotocol StreamableRequestBody
  "A protocol for reading the request body as an input stream."
  (-body-stream [body request]))

(defn body-stream
  "Given a Ring 2 request map, return an input stream to read the body."
  [request]
  (-body-stream (::body request) request))

(extend-protocol StreamableRequestBody
  java.io.InputStream
  (-body-stream [stream _] stream))

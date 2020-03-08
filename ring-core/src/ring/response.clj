(ns ring.response
  "Essential protocols for Ring 2 response maps."
  {:added "2.0"})

(defprotocol StreamableResponseBody
  "A protocol for writing data to the response body via an output stream."
  (-write-body-to-stream [body response output-stream]))

(defn write-body-to-stream
  "Write a value representing a response body to an output stream. The stream
  will be automically closed after the function ends."
  [response output-stream]
  (-write-body-to-stream (::body response) response output-stream))

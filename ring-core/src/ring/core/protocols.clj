(ns ring.core.protocols
  (:require [clojure.java.io :as io]))

(defprotocol StreamingResponseBody
  "A protocol for writing data to the response body via an output stream."
  (write-body-to-stream [body output-stream]
    "Write a value representing a response body to an output stream."))

(extend-protocol StreamingResponseBody
  String
  (write-body-to-stream [body output-stream]
    (with-open [writer (io/writer output-stream)]
      (.write writer body)))
  clojure.lang.ISeq
  (write-body-to-stream [body output-stream]
    (with-open [writer (io/writer output-stream)]
      (doseq [chunk body]
        (.write writer (str chunk)))))
  java.io.InputStream
  (write-body-to-stream [body output-stream]
    (with-open [out output-stream, body body]
      (io/copy body out)))
  java.io.File
  (write-body-to-stream [body output-stream]
    (with-open [out output-stream]
      (io/copy body out)))
  nil
  (write-body-to-stream [_ ^java.io.OutputStream output-stream]
    (.close output-stream)))

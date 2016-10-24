(ns ring.core.protocols
  "Protocols necessary for Ring."
  {:added "1.6"}
  (:import [java.io Writer OutputStream])
  (:require [clojure.java.io :as io]
            [ring.util.response :as response]))

(defprotocol ^{:added "1.6"} StreamableResponseBody
  "A protocol for writing data to the response body via an output stream."
  (write-body-to-stream [body response output-stream]
    "Write a value representing a response body to an output stream. The stream
    will be closed after the value had been written."))

(defn- ^Writer response-writer [response output-stream]
  (if-let [charset (response/get-charset response)]
    (io/writer output-stream :encoding charset)
    (io/writer output-stream)))

(extend-protocol StreamableResponseBody
  String
  (write-body-to-stream [body response output-stream]
    (with-open [writer (response-writer response output-stream)]
      (.write writer body)))
  clojure.lang.ISeq
  (write-body-to-stream [body response output-stream]
    (with-open [writer (response-writer response output-stream)]
      (doseq [chunk body]
        (.write writer (str chunk)))))
  java.io.InputStream
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (with-open [out output-stream, body body]
      (io/copy body out)))
  java.io.File
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (with-open [out output-stream]
      (io/copy body out)))
  nil
  (write-body-to-stream [_ _ ^java.io.OutputStream output-stream]
    (.close output-stream)))

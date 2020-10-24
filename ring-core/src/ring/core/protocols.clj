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
  (Class/forName "[B")
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (.write output-stream ^bytes body)
    (.close output-stream))
  String
  (write-body-to-stream [body response output-stream]
    (doto (response-writer response output-stream)
      (.write body)
      (.close)))
  clojure.lang.ISeq
  (write-body-to-stream [body response output-stream]
    (let [writer (response-writer response output-stream)]
      (doseq [chunk body]
        (.write writer (str chunk)))
      (.close writer)))
  java.io.InputStream
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (with-open [body body]
      (io/copy body output-stream))
    (.close output-stream))
  java.io.File
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (io/copy body output-stream)
    (.close output-stream))
  nil
  (write-body-to-stream [_ _ ^java.io.OutputStream output-stream]
    (.close output-stream)))

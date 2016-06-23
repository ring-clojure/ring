(ns ring.core.protocols
  (:require [clojure.java.io :as io]))

(defprotocol ResponseBody
  "A protocol for writing data to the response body."
  (write-body [body output-stream]
    "Write a value representing a response body to an output stream."))

(extend-protocol ResponseBody
  String
  (write-body [body output]
    (with-open [writer (io/writer output)]
      (.write writer body)))
  clojure.lang.ISeq
  (write-body [body output]
    (with-open [writer (io/writer output)]
      (doseq [chunk body]
        (.write writer (str chunk)))))
  java.io.InputStream
  (write-body [body output]
    (with-open [input body]
      (io/copy input output)))
  java.io.File
  (write-body [body output]
    (io/copy body output))
  nil
  (write-body [_ _]))

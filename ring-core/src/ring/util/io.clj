(ns ring.util.io
  "Utility functions for I/O in Ring."
  (:require [clojure.java.io :as io])
  (:import [java.io PipedInputStream
                    PipedOutputStream
                    ByteArrayInputStream
                    Closeable
                    IOException]))

(defn piped-input-stream
  "Create an input stream from a function that takes an output stream as its
  argument. The function will be executed in a separate thread. The stream
  will be automatically closed after the function finishes.

  e.g. (piped-input-stream
        (fn [ostream]
          (spit ostream \"Hello\")))"
  [func]
  (let [input  (PipedInputStream.)
        output (PipedOutputStream.)]
    (.connect input output)
    (future
      (try
        (func output)
        (finally (.close output))))
    input))

(defn string-input-stream
  "Returns a ByteArrayInputStream for the given String."
  ([^String s]
     (ByteArrayInputStream. (.getBytes s)))
  ([^String s ^String encoding]
     (ByteArrayInputStream. (.getBytes s encoding))))

(defn close!
  "Ensure a stream is closed, swallowing any exceptions."
  [stream]
  (when (instance? java.io.Closeable stream)
    (try
      (.close ^java.io.Closeable stream)
      (catch IOException _ nil))))

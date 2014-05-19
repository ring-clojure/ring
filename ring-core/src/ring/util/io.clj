(ns ring.util.io
  "Utility functions for handling I/O."
  (:require [clojure.java.io :as io])
  (:import [java.io PipedInputStream
                    PipedOutputStream
                    ByteArrayInputStream
                    File
                    Closeable
                    IOException]))

(defn piped-input-stream
  "Create an input stream from a function that takes an output stream as its
  argument. The function will be executed in a separate thread. The stream
  will be automatically closed after the function finishes.

  For example:

    (piped-input-stream
      (fn [ostream]
        (spit ostream \"Hello\")))"
  {:added "1.1"}
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
  {:added "1.1"}
  ([^String s]
     (ByteArrayInputStream. (.getBytes s)))
  ([^String s ^String encoding]
     (ByteArrayInputStream. (.getBytes s encoding))))

(defn close!
  "Ensure a stream is closed, swallowing any exceptions."
  {:added "1.2"}
  [stream]
  (when (instance? java.io.Closeable stream)
    (try
      (.close ^java.io.Closeable stream)
      (catch IOException _ nil))))

(defn last-modified-date
  "Returns the last modified date for a file, rounded down to the nearest
  second."
  {:added "1.2"}
  [^File file]
  (-> (.lastModified file)
      (/ 1000) (long) (* 1000)
      (java.util.Date.)))

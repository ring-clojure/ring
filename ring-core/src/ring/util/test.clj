(ns ring.util.test
  "Utilities for testing Ring components."
  (:import java.io.ByteArrayInputStream))

(defn string-input-stream
  "Returns a ByteArrayInputStream for the given String."
  ([^String s]
     (ByteArrayInputStream. (.getBytes s)))
  ([^String s encoding]
     (ByteArrayInputStream. (.getBytes s encoding))))

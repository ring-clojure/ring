(ns ring.util.test
  "Utilities for testing Ring components."
  (:import (java.io ByteArrayInputStream)))

(defn string-input-stream [#^String s]
  "Returns a ByteArrayInputStream for the given String."
  (ByteArrayInputStream. (.getBytes s)))

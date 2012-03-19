(ns ring.util.test
  "Utilities for testing Ring components."
  (:require [ring.util.io :as io]))

(def ^{:doc "Returns a ByteArrayInputStream for the given String."
       :deprecated "1.1"}
  string-input-stream
  io/string-input-stream)

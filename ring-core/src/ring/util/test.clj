(ns ring.util.test
  "Utilities for testing Ring components.

  All functions in this namespace are currently deprecated."
  {:deprecated "1.1"}
  (:require [ring.util.io :as io]))

(def ^{:doc "Returns a ByteArrayInputStream for the given String.

  See: ring.util.io/string-input-stream."
       :deprecated "1.1"}
  string-input-stream
  io/string-input-stream)

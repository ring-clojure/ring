(ns ring.util.request
  "Parse and interpret Ring requests"
  (:require [clojure.string :as str]))

(defn header-seq
  "Returns seq of values for the specified header. Multiple values can be
  encoded in one header key using a comma as a separator."
  [request key]
  (-> (get-in request [:headers (name key)])
      (str/split #"\s*,\s*")))

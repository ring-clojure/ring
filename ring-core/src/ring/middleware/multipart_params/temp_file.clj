(ns ring.middleware.multipart-params.temp-file
  (:require [clojure.java.io :as io])
  (:import java.io.File))

(defn temp-file-store
  "Stores multipart file parameters as a temporary file."
  [item]
  (let [temp-file (File/createTempFile "ring-multipart-" "")]
    (with-open [out (io/output-stream temp-file)]
      (io/copy (:stream item) out))
    (-> (select-keys item [:filename :content-type])
        (assoc :temp-file temp-file))))

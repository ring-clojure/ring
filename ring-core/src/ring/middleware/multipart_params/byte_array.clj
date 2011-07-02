(ns ring.middleware.multipart-params.byte-array
  (:import org.apache.commons.io.IOUtils))

(defn byte-array-store
  "Stores multipart file parameters as an array of bytes."
  [item]
  (-> (select-keys item [:filename :content-type])
      (assoc :bytes (IOUtils/toByteArray (:stream item)))))

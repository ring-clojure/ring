(ns ring.middleware.multipart-params.byte-array
  (:import org.apache.commons.io.IOUtils))

(defn byte-array-store
  "Returns a function that stores multipart file parameters as an array of
  bytes. The multipart parameters will be stored as maps with the following
  keys:
    :filename     - the name of the uploaded file
    :content-type - the content type of the uploaded file
    :bytes        - an array of bytes containing the uploaded content"
  []
  (fn [item]
    (-> (select-keys item [:filename :content-type])
        (assoc :bytes (IOUtils/toByteArray ^java.io.InputStream (:stream item))))))

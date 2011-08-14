(ns ring.middleware.multipart-params.temp-file
  (:require [clojure.java.io :as io])
  (:import java.io.File))

(defmacro ^{:private true} do-every [delay & body]
  `(future
     (while true
       (Thread/sleep (* ~delay 1000))
       (try ~@body
            (catch Exception ex#)))))

(defn- expired? [^File file expiry-time]
  (< (.lastModified file)
     (- (System/currentTimeMillis)
        (* expiry-time 1000))))

(defn- remove-old-files [file-set expiry-time]
  (doseq [^File file @file-set]
    (when (expired? file expiry-time)
      (.delete file)
      (swap! file-set disj file))))

(defn- ^File make-temp-file [file-set]
  (let [temp-file (File/createTempFile "ring-multipart-" nil)]
    (swap! file-set conj temp-file)
    (.deleteOnExit temp-file)
    temp-file))

(defn temp-file-store
  "Returns a function that stores multipart file parameters as temporary files.
  Accepts the following options:
    :expires-in - delete temporary files older than this many seconds
                  (defaults to 3600 - 1 hour)
  The multipart parameters will be stored as maps with the following keys:
    :filename     - the name of the uploaded file
    :content-type - the content type of the upload file
    :tempfile     - a File object that points to the temporary file containing
                    the uploaded data.
    :size         - the size in bytes of the uploaded data"
  ([] (temp-file-store {:expires-in 3600}))
  ([{:keys [expires-in]}]
     (fn [item]
       (let [file-set  (atom #{})
             temp-file (make-temp-file file-set)]
         (io/copy (:stream item) temp-file)
         (when expires-in
           (do-every expires-in
             (remove-old-files file-set expires-in)))
         (-> (select-keys item [:filename :content-type])
             (assoc :tempfile temp-file
                    :size (.length temp-file)))))))

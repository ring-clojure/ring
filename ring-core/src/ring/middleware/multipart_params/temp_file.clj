(ns ring.middleware.multipart-params.temp-file
  (:require [clojure.java.io :as io])
  (:import java.io.File))

(defmacro ^{:private true} do-every [delay & body]
  `(future
     (while true
       (Thread/sleep (* ~delay 1000))
       (try ~@body
            (catch Exception ex#)))))

(defn- expired? [file expiry-time]
  (< (.lastModified file)
     (- (System/currentTimeMillis)
        (* expiry-time 1000))))

(defn- remove-old-files [file-set expiry-time]
  (doseq [file @file-set]
    (when (expired? file expiry-time)
      (.delete file)
      (swap! file-set disj file))))

(defn- make-temp-file [file-set]
  (let [temp-file (File/createTempFile "ring-multipart-" nil)]
    (swap! file-set conj temp-file)
    (.deleteOnExit temp-file)
    temp-file))

(defn temp-file-store
  "Stores multipart file parameters as a temporary file."
  ([] (temp-file-store {:expire-in 3600}))
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

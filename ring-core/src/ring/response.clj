(ns ring.response
  "Core protocols and functions for Ring 2 response maps."
  {:added "2.0"}
  (:require [clojure.java.io :as io]
            [ring.util.parsing :as parsing]))

(defprotocol StreamableResponseBody
  "A protocol for writing data to the response body via an output stream."
  (-write-body-to-stream [body response output-stream]))

(defn write-body-to-stream
  "Write a value representing a response body to an output stream. The stream
  will be automically closed after the function ends."
  [response output-stream]
  (-write-body-to-stream (::body response) response output-stream))

(defn- get-charset [request]
  (when-let [content-type (-> request ::headers (get "content-type"))]
    (second (re-find parsing/re-charset content-type))))

(defn- ^java.io.Writer response-writer [response output-stream]
  (if-let [charset (get-charset response)]
    (io/writer output-stream :encoding charset)
    (io/writer output-stream)))

(extend-protocol StreamableResponseBody
  (Class/forName "[B")
  (-write-body-to-stream [body _ ^java.io.OutputStream output-stream]
    (with-open [out output-stream]
      (.write out ^bytes body)))
  String
  (-write-body-to-stream [body response output-stream]
    (with-open [writer (response-writer response output-stream)]
      (.write writer body)))
  clojure.lang.ISeq
  (-write-body-to-stream [body response output-stream]
    (with-open [writer (response-writer response output-stream)]
      (doseq [chunk body]
        (.write writer (str chunk)))))
  java.io.InputStream
  (-write-body-to-stream [body _ ^java.io.OutputStream output-stream]
    (with-open [out output-stream, body body]
      (io/copy body out)))
  java.io.File
  (-write-body-to-stream [body _ ^java.io.OutputStream output-stream]
    (with-open [out output-stream]
      (io/copy body out)))
  nil
  (-write-body-to-stream [_ _ ^java.io.OutputStream output-stream]
    (.close output-stream)))

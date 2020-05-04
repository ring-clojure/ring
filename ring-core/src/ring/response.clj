(ns ring.response
  "Core protocols and functions for Ring 2 response maps."
  {:added "2.0"}
  (:require [clojure.java.io :as io]
            [ring.util.parsing :as parsing]))

(defprotocol Response
  "A protocol representing a HTTP response. By default satisfied by maps that
  use either Ring 1 or Ring 2 keys."
  (status  [resp])
  (headers [resp])
  (body    [resp]))

(defprotocol StreamableResponseBody
  "A protocol for writing data to the response body via an output stream."
  (-write-body-to-stream [body response output-stream]))

(defn write-body-to-stream
  "Write a value representing a response body to an output stream. The stream
  will be automically closed after the function ends."
  [response output-stream]
  (-write-body-to-stream (body response) response output-stream))

(defn charset
  "Given a response map, return the charset of the content-type header."
  [response]
  (when-let [content-type (-> response headers (get "content-type"))]
    (second (re-find parsing/re-charset content-type))))

(defn- ^java.io.Writer response-writer [response output-stream]
  (if-let [encoding (charset response)]
    (io/writer output-stream :encoding encoding)
    (io/writer output-stream)))

(extend-protocol Response
  clojure.lang.IPersistentMap
  (status  [resp] (::status  resp (:status resp)))
  (headers [resp] (::headers resp (:headers resp)))
  (body    [resp] (::body    resp (:body resp))))

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

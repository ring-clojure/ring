(ns ring.core.protocols
  "Protocols necessary for Ring."
  {:added "1.6"}
  (:import [java.io OutputStream OutputStreamWriter Writer])
  (:require [clojure.java.io :as io]))

(defprotocol ^{:added "1.6"} StreamableResponseBody
  "A protocol for writing data to the response body via an output stream."
  (write-body-to-stream [body response output-stream]
    "Write a value representing a response body to an output stream. The stream
    will be closed after the value had been written. The stream may be written
    asynchronously from asynchronous handlers. In synchronous handlers, the
    response is considered completed once this method ends."))

;; The following private functions are replicated from ring.util.response in
;; order to allow third-party adapters to use StreamableResponseBody without the
;; need for a ring-core dependency.

(def ^:private re-charset
  #"(?x);(?:.*\s)?(?i:charset)=(?:
      ([!\#$%&'*\-+.0-9A-Z\^_`a-z\|~]+)|  # token
      \"((?:\\\"|[^\"])*)\"               # quoted
    )\s*(?:;|$)")

(defn- find-charset-in-content-type [content-type]
  (when-let [m (re-find re-charset content-type)]
    (or (m 1) (m 2))))

(defn- response-charset ^String [response]
  (some->> (:headers response)
           (some #(when (.equalsIgnoreCase "content-type" (key %)) (val %)))
           (find-charset-in-content-type)))

(defn- str->bytes ^bytes [^String s ^String charset]
  (if charset (.getBytes s charset) (.getBytes s)))

(defn- response-writer ^Writer [response ^OutputStream output-stream]
  (if-let [charset (response-charset response)]
    (OutputStreamWriter. output-stream charset)
    (OutputStreamWriter. output-stream)))

;; Extending primitive arrays prior to Clojure 1.12 requires using the low-level
;; extend function.
(extend (Class/forName "[B")
  StreamableResponseBody
  {:write-body-to-stream
   (fn [body _ ^OutputStream output-stream]
     (.write output-stream ^bytes body)
     (.close output-stream))})

;; Note: output streams are deliberately not closed on error, so that the
;; adapter or error middleware can potentially send extra error information to
;; the client.

(extend-protocol StreamableResponseBody
  String
  (write-body-to-stream [body response ^OutputStream output-stream]
    ;; No need to use a writer for a single string, and this prevents a
    ;; flush being used for a value of fixed length.
    (.write output-stream (str->bytes body (response-charset response)))
    (.close output-stream))
  clojure.lang.ISeq
  (write-body-to-stream [body response output-stream]
    (let [writer (response-writer response output-stream)]
      (doseq [chunk body]
        (.write writer (str chunk))
        (.flush writer))
      (.close writer)))
  java.io.InputStream
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (with-open [body body]
      (io/copy body output-stream))
    (.close output-stream))
  java.io.File
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (io/copy body output-stream)
    (.close output-stream))
  nil
  (write-body-to-stream [_ _ ^java.io.OutputStream output-stream]
    (.close output-stream)))

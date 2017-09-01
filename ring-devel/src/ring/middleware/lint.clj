(ns ring.middleware.lint
  "Middleware that checks Ring requests and responses for correctness."
  (:require [clojure.set :as set]
            [clojure.string :as str])
  (:import [java.io File InputStream]
           [java.security.cert X509Certificate]))

(defn- lint
  "Asserts that spec applied to val returns logical truth, otherwise raises
  an exception with a message produced by applying format to the message-pattern
  argument and a printing of an invalid val."
  [val spec message]
  (try
    (if-not (spec val)
      (throw (Exception. (format "Ring lint error: specified %s, but %s was not"
                                 message (pr-str val)))))
    (catch Exception e
      (if-not (re-find #"^Ring lint error: " (.getMessage e))
        (throw (Exception. (format
          "Ring lint error: exception occurred when checking that %s on %s: %s"
          message (pr-str val) (.getMessage e))))
        (throw e)))))

(defn- check-req
  "Validates the request, throwing an exception on violations of the spec."
  [req]
  (lint req map? "Ring request must a Clojure map")

  (lint (:server-port req) integer?
    ":server-port must be an Integer")
  (lint (:server-name req) string?
    ":server-name must be a String")
  (lint (:remote-addr req) string?
    ":remote-addr must be a String")
  (lint (:uri req) #(and (string? %) (.startsWith ^String % "/"))
    ":uri must be a String starting with \"/\"")
  (lint (:query-string req) #(or (nil? %) (string? %))
    ":query-string must be nil or a String")
  (lint (:scheme req) #{:http :https}
    ":scheme must be one of :http or :https")
  (lint (:request-method req) #(and (keyword? %) (= (name %) (str/lower-case (name %))))
    ":request-method must be a lowercase keyword")
  (lint (:content-type req) #(or (nil? %) (string? %))
    ":content-type must be nil or a String")
  (lint (:content-length req) #(or (nil? %) (integer? %))
    ":content-length must be nil or an Integer")
  (lint (:character-encoding req) #(or (nil? %) (string? %))
    ":character-encoding must be nil or a String")
  (lint (:ssl-client-cert req) #(or (nil? %) (instance? X509Certificate %))
    ":ssl-client-cert must be nil or an X509Certificate")

  (let [headers (:headers req)]
    (lint headers map?
      ":headers must be a Clojure map")
    (doseq [[hname hval] headers]
      (lint hname string?
         "header names must be Strings")
      (lint hname #(= % (.toLowerCase ^String %))
        "header names must be in lower case")
      (lint hval string?
        "header values must be strings")))

  (lint (:body req) #(or (nil? %) (instance? InputStream %))
    ":body must be nil or an InputStream"))

(defn- check-resp
  "Validates the response, throwing an exception on violations of the spec"
  [resp]
  (lint resp map?
    "Ring response must be a Clojure map")

  (lint (:status resp) #(and (integer? %) (>= % 100))
    ":status must be an Integer greater than or equal to 100")

  (let [headers (:headers resp)]
    (lint headers map?
      ":headers must be a Clojure map")
    (doseq [[hname hval] headers]
      (lint hname string?
        "header names must Strings")
      (lint hval #(or (string? %) (every? string? %))
        "header values must be Strings or collections of Strings")))

  (lint (:body resp) #(or (nil? %)
                          (string? %)
                          (seq? %)
                          (instance? File %)
                          (instance? InputStream %))
    ":body must a String, ISeq, File, or InputStream"))

(defn wrap-lint
  "Wrap a handler to validate incoming requests and outgoing responses
  according to the current Ring specification. An exception is raised if either
  the request or response is invalid."
  [handler]
  (fn
    ([request]
     (check-req request)
     (let [response (handler request)]
       (check-resp response)
       response))
    ([request respond raise]
     (check-req request)
     (handler request
              (fn [response]
                (check-resp response)
                (respond response))
              raise))))

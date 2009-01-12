(ns ring.lint
  (:use clojure.set clojure.contrib.except)
  (:import (java.io File InputStream)))

(defn lint
  "Asserts that spec applied to val returns logical truth, otherwise raises
  an exception with a message produced by applying format to the message-pattern
  argument and a printing of an invalid val."
  [val spec message]
  (try
    (if-not (spec val)
      (throwf "Ring lint error: specified %s, but %s was not" message (pr-str val)))
    (catch Exception e
      (if-not (re-find #"^Ring lint error: " (.getMessage e))
        (throwf
          "Ring lint error: exception occured when checking that %s on %s: %s"
          message (pr-str val) (.getMessage e))
        (throw e)))))

(defn lint-namespacing
  "Asserts that all keys are namespaces other than those included in a 
  specified set of permitted unnamspaced keys"
  [map map-name no-namespace-needed]
  (let [must-namespace (difference (set (keys map)) no-namespace-needed)]
    (doseq [k must-namespace]
      (lint k namespace
        (format "user keys in the %s map must be namespaced" map-name)))))

(defn check-req
  "Validates the request, throwing an exception on violations of the spec"
  [req]
  (lint req map?
    "Ring request must a Clojure map")

  (lint (:server-port req) integer?
    ":server-port must be an Integer")
  (lint (:server-name req) string?
    ":server-name must be a String")
  (lint (:remote-addr req) string?
    ":remote-addr must be a String")
  (lint (:uri req) #(and (string? %) (.startsWith % "/"))
    ":uri must be a String starting with \"/\"")
  (lint (:query-string req) #(or (nil? %) (string? %))
    ":query-string must be nil or a non-blank String")
  (lint (:scheme req) #{:http :https}
    ":scheme must be one of :http or :https")
  (lint (:request-method req) #{:get :head :options :put :post :delete}
    ":request-method must be one of :get, :head, :options, :put, :post, or :delete")
  (lint (:content-type req) #(or (nil? %) (string? %))
    ":content-type must be nil or a String")
  (lint (:content-length req) #(or (nil? %) (integer? %))
    ":content-length must be nil or an Integer")
  (lint (:character-encoding req) #(or (nil? %) (string? %))
    ":character-encoding must be nil or a String")

  (let [headers (:headers req)]
    (lint headers map?
      ":headers must be a Clojure map")
    (doseq [[hname hval] headers]
      (lint hname string?
         "header names must be Strings")
      (lint hname #(= % (.toLowerCase %))
        "header names must be in lower case")
      (lint hval string?
        "header values must be strings")))

  (lint (:body req) #(or (nil? %) (instance? InputStream %))
    ":body must be nil or an InputStream")

  (lint-namespacing req "request"
    #{:server-port :server-name :remote-addr :uri :query-string :scheme
      :request-method :content-type :content-length :character-encoding
      :headers :body}))

(defn check-resp
  "Validates the response, throwing an exception on violations of the spec"
  [resp]
  (lint resp map?
    "Ring response must be a Clojure map")

  (lint (:status resp) #(and (integer? %) (>= % 100))
    ":status must be an Intger greater than or equal to 100")

  (let [headers (:headers resp)]
    (lint headers map?
      ":headers must be a Clojure map")
    (doseq [[hname hval] headers]
      (lint hname string?
        "header names must Strings")
      (lint hval #(or (string? %) (every? string? %))
        "header values must be Strings or colls of Strings")))

  (lint (:body resp) #(or (nil? %) (string? %) (instance? File %)
                          (instance? InputStream %))
    ":body must a String, File, or InputStream")

  (lint-namespacing resp "response"
    #{:status :headers :body}))

(defn wrap
  "Wrap an app to validate incoming requests and outgoing responses
  according to the Ring spec."
  [app]
  (fn [req]
    (check-req req)
    (let [resp (app req)]
      (check-resp resp)
      resp)))

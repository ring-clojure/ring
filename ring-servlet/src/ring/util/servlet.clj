(ns ring.util.servlet
  "Compatibility functions for turning a ring handler into a Java servlet."
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.io File InputStream FileInputStream)
           (javax.servlet.http HttpServlet
                               HttpServletRequest
                               HttpServletResponse)))

(defn- get-headers
  "Creates a name/value map of all the request headers."
  [^HttpServletRequest request]
  (reduce
    (fn [headers, ^String name]
      (assoc headers
        (.toLowerCase name)
        (->> (.getHeaders request name)
             (enumeration-seq)
             (string/join ","))))
    {}
    (enumeration-seq (.getHeaderNames request))))

(defn- get-content-length
  "Returns the content length, or nil if there is no content."
  [^HttpServletRequest request]
  (let [length (.getContentLength request)]
    (if (>= length 0) length)))

(defn- get-client-cert
  "Returns the SSL client certificate of the request, if one exists."
  [^HttpServletRequest request]
  (first (.getAttribute request "javax.servlet.request.X509Certificate")))

(defn build-request-map
  "Create the request map from the HttpServletRequest object."
  [^HttpServletRequest request]
  {:server-port        (.getServerPort request)
   :server-name        (.getServerName request)
   :remote-addr        (.getRemoteAddr request)
   :uri                (.getRequestURI request)
   :query-string       (.getQueryString request)
   :scheme             (keyword (.getScheme request))
   :request-method     (keyword (.toLowerCase (.getMethod request)))
   :protocol           (.getProtocol request)
   :headers            (get-headers request)
   :content-type       (.getContentType request)
   :content-length     (get-content-length request)
   :character-encoding (.getCharacterEncoding request)
   :ssl-client-cert    (get-client-cert request)
   :body               (.getInputStream request)})

(defn merge-servlet-keys
  "Associate servlet-specific keys with the request map for use with legacy
  systems."
  [request-map
   ^HttpServlet servlet
   ^HttpServletRequest request
   ^HttpServletResponse response]
  (merge request-map
         {:servlet              servlet
          :servlet-request      request
          :servlet-response     response
          :servlet-context      (.getServletContext servlet)
          :servlet-context-path (.getContextPath request)}))

(defn- set-status
  "Update a HttpServletResponse with a status code."
  [^HttpServletResponse response, status]
  (.setStatus response status))

(defn- set-headers
  "Update a HttpServletResponse with a map of headers."
  [^HttpServletResponse response, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val))))
  ; Some headers must be set through specific methods
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType response content-type)))

(defn- set-body
  "Update a HttpServletResponse body with a String, ISeq, File or InputStream."
  [^HttpServletResponse response, body]
  (cond
    (string? body)
      (with-open [writer (.getWriter response)]
        (.print writer body))
    (seq? body)
      (with-open [writer (.getWriter response)]
        (doseq [chunk body]
          (.print writer (str chunk))))
    (instance? InputStream body)
      (with-open [^InputStream b body]
        (io/copy b (.getOutputStream response)))
    (instance? File body)
      (let [^File f body]
        (with-open [stream (FileInputStream. f)]
          (set-body response stream)))
    (nil? body)
      nil
    :else
      (throw (Exception. ^String (format "Unrecognized body: %s" body)))))

(defn update-servlet-response
  "Update the HttpServletResponse using a response map."
  {:arglists '([response response-map])}
  [^HttpServletResponse response, {:keys [status headers body]}]
  (when-not response
    (throw (Exception. "Null response given.")))
  (when status
    (set-status response status))
  (doto response
    (set-headers headers)
    (set-body body)))

(defn make-service-method
  "Turns a handler into a function that takes the same arguments and has the
  same return value as the service method in the HttpServlet class."
  [handler]
  (fn [^HttpServlet servlet
      ^HttpServletRequest request
      ^HttpServletResponse response]
    (let [request-map (-> request
                          (build-request-map)
                          (merge-servlet-keys servlet request response))]
      (if-let [response-map (handler request-map)]
        (update-servlet-response response response-map)
        (throw (NullPointerException. "Handler returned nil"))))))

(defn servlet
  "Create a servlet from a Ring handler."
  [handler]
  (let [service-method (make-service-method handler)]
    (proxy [HttpServlet] []
      (service [request response]
        (service-method this request response)))))

(defmacro defservice
  "Defines a service method with an optional prefix suitable for being used by
  genclass to compile a HttpServlet class.

  For example:

    (defservice my-handler)
    (defservice \"my-prefix-\" my-handler)"
  ([handler]
     `(defservice "-" ~handler))
  ([prefix handler]
     `(let [service-method# (make-service-method ~handler)]
        (defn ~(symbol (str prefix "service"))
          [servlet# request# response#]
          (service-method# servlet# request# response#)))))

(ns ring.util.servlet
  "Compatibility functions for turning a ring handler into a Java servlet."
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [ring.core.protocols :as protocols]
            [ring.request :as req]
            [ring.response :as resp])
  (:import [java.io File InputStream FileInputStream]
           [java.util Locale]
           [javax.servlet AsyncContext]
           [javax.servlet.http HttpServlet
                               HttpServletRequest
                               HttpServletResponse]))

(defn- build-header-map [^HttpServletRequest request]
  (reduce
   (fn [headers ^String name]
     (assoc headers
            (.toLowerCase name Locale/ENGLISH)
            (->> (.getHeaders request name)
                 (enumeration-seq)
                 (string/join ","))))
   {}
   (enumeration-seq (.getHeaderNames request))))

(defn- get-content-length [^HttpServletRequest request]
  (let [length (.getContentLength request)]
    (if (>= length 0) length)))

(defn- get-client-cert [^HttpServletRequest request]
  (first (.getAttribute request "javax.servlet.request.X509Certificate")))

(defn- get-request-method [^HttpServletRequest request]
  (keyword (.toLowerCase (.getMethod request) Locale/ENGLISH)))

(defn build-request-map
  "Create a Ring 1 request map from a HttpServletRequest object."
  [^HttpServletRequest request]
  {:server-port        (.getServerPort request)
   :server-name        (.getServerName request)
   :remote-addr        (.getRemoteAddr request)
   :uri                (.getRequestURI request)
   :query-string       (.getQueryString request)
   :scheme             (keyword (.getScheme request))
   :request-method     (get-request-method request)
   :protocol           (.getProtocol request)
   :headers            (build-header-map request)
   :content-type       (.getContentType request)
   :content-length     (get-content-length request)
   :character-encoding (.getCharacterEncoding request)
   :ssl-client-cert    (get-client-cert request)
   :body               (.getInputStream request)})

(defn- build-header-map-2 [^HttpServletRequest request]
  (reduce
   (fn [headers ^String name]
     (assoc headers
            (.toLowerCase name Locale/ENGLISH)
            (-> request (.getHeaders name) enumeration-seq vec)))
   {}
   (enumeration-seq (.getHeaderNames request))))

(defn build-request-map-2
  "Create a Ring 2 request map from a HttpServletRequest object."
  [^HttpServletRequest request]
  #::req{:server-port     (.getServerPort request)
         :server-name     (.getServerName request)
         :remote-addr     (.getRemoteAddr request)
         :path            (.getRequestURI request)
         :query           (.getQueryString request)
         :scheme          (keyword (.getScheme request))
         :method          (get-request-method request)
         :protocol        (.getProtocol request)
         :headers         (build-header-map-2 request)
         :ssl-client-cert (get-client-cert request)
         :body            (.getInputStream request)})

(defn merge-servlet-keys
  "Associate servlet-specific keys with a Ring 1 request map for use with
  legacy systems."
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

(defn merge-servlet-keys-2
  "Associate servlet-specific keys with a Ring 2 request map for use with
  legacy systems."
  [request-map
   ^HttpServlet servlet
   ^HttpServletRequest request
   ^HttpServletResponse response]
  (merge request-map
         #:ring.servlet{:servlet      servlet
                        :request      request
                        :response     response
                        :context      (.getServletContext servlet)
                        :context-path (.getContextPath request)}))

(defn- set-headers [^HttpServletResponse response, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val))))
  ; Some headers must be set through specific methods
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType response content-type)))

(defn- make-output-stream [^HttpServletResponse response ^AsyncContext context]
  (let [os (.getOutputStream response)]
    (if (nil? context)
      os
      (proxy [java.io.FilterOutputStream] [os]
        (close []
          (.close os)
          (.complete context))))))

(defn update-servlet-response
  "Update the HttpServletResponse using a Ring 1 response map. Takes an
  optional AsyncContext."
  ([response response-map]
   (update-servlet-response response nil response-map))
  ([^HttpServletResponse response context response-map]
   (let [{:keys [status headers body]} response-map]
     (when (nil? response)
       (throw (NullPointerException. "HttpServletResponse is nil")))
     (when (nil? response-map)
       (throw (NullPointerException. "Response map is nil")))
     (when status
       (.setStatus response status))
     (set-headers response headers)
     (let [output-stream (make-output-stream response context)]
       (protocols/write-body-to-stream body response-map output-stream)))))

(defn update-servlet-response-2
  "Update the HttpServletResponse using a Ring 2 response map. Takes an
  optional AsyncContext."
  ([response response-map]
   (update-servlet-response-2 response nil response-map))
  ([^HttpServletResponse response context response-map]
   (let [{:ring.response/keys [status headers body]} response-map]
     (when (nil? response)
       (throw (NullPointerException. "HttpServletResponse is nil")))
     (when (nil? response-map)
       (throw (NullPointerException. "Response map is nil")))
     (.setStatus response status)
     (doseq [[k vs] headers, v vs]
       (.addHeader response k v))
     (let [output-stream (make-output-stream response context)]
       (resp/write-body-to-stream response-map output-stream)))))

(defn- make-blocking-service-method [handler]
  (fn [servlet request response]
    (-> request
        (build-request-map)
        (merge-servlet-keys servlet request response)
        (handler)
        (->> (update-servlet-response response)))))

(defn- make-async-service-method [handler]
  (fn [servlet ^HttpServletRequest request ^HttpServletResponse response]
    (let [^AsyncContext context (.startAsync request)]
      (handler
       (-> request
           (build-request-map)
           (merge-servlet-keys servlet request response))
       (fn [response-map]
         (update-servlet-response response context response-map))
       (fn [^Throwable exception]
         (.sendError response 500 (.getMessage exception))
         (.complete context))))))

(defn- make-blocking-service-method-2 [handler] 
  (fn [servlet request response]
    (-> request
        (build-request-map-2)
        (merge-servlet-keys-2 servlet request response)
        (handler)
        (->> (update-servlet-response-2 response)))))

(defn- make-async-service-method-2 [handler]
  (fn [servlet ^HttpServletRequest request ^HttpServletResponse response]
    (let [^AsyncContext context (.startAsync request)]
      (handler
       (-> request
           (build-request-map-2)
           (merge-servlet-keys-2 servlet request response))
       (fn [response-map]
         (update-servlet-response-2 response context response-map))
       (fn [^Throwable exception]
         (.sendError response 500 (.getMessage exception))
         (.complete context))))))

(defn make-service-method
  "Turns a handler into a function that takes the same arguments and has the
  same return value as the service method in the HttpServlet class."
  ([handler]
   (make-service-method handler {}))
  ([handler options]
   (if (= 2 (:ring options))
     (if (:async? options)
       (make-async-service-method-2 handler)
       (make-blocking-service-method-2 handler)) 
     (if (:async? options)
       (make-async-service-method handler)
       (make-blocking-service-method handler)))))

(defn servlet
  "Create a servlet from a Ring handler."
  ([handler]
   (servlet handler {}))
  ([handler options]
   (let [service-method (make-service-method handler options)]
     (proxy [HttpServlet] []
       (service [request response]
         (service-method this request response))))))

(defmacro defservice
  "Defines a service method with an optional prefix suitable for being used by
  genclass to compile a HttpServlet class.

  For example:

    (defservice my-handler)
    (defservice \"my-prefix-\" my-handler)"
  ([handler]
     `(defservice "-" ~handler))
  ([prefix handler]
   (if (map? handler)
     `(defservice "-" ~prefix ~handler)
     `(defservice ~prefix ~handler {})))
  ([prefix handler options]
     `(let [service-method# (make-service-method ~handler ~options)]
        (defn ~(symbol (str prefix "service"))
          [servlet# request# response#]
          (service-method# servlet# request# response#)))))

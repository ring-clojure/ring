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

(defn- build-header-map-1 [^HttpServletRequest request]
  (persistent!
   (reduce
    (fn [headers ^String name]
      (assoc! headers
              (.toLowerCase name Locale/ENGLISH)
              (->> (.getHeaders request name)
                   (enumeration-seq)
                   (string/join ","))))
    (transient {})
    (enumeration-seq (.getHeaderNames request)))))

(defn- build-header-map-2 [^HttpServletRequest request]
  (persistent!
   (reduce
    (fn [headers ^String name]
      (assoc! headers
              (.toLowerCase name Locale/ENGLISH)
              (-> request (.getHeaders name) enumeration-seq vec)))
    (transient {})
    (enumeration-seq (.getHeaderNames request)))))

(defn- get-content-length [^HttpServletRequest request]
  (let [length (.getContentLength request)]
    (if (>= length 0) length)))

(defn- get-client-cert [^HttpServletRequest request]
  (first (.getAttribute request "javax.servlet.request.X509Certificate")))

(defn- get-request-method [^HttpServletRequest request]
  (keyword (.toLowerCase (.getMethod request) Locale/ENGLISH)))

(defn- assoc-request-fields-1! [request-map ^HttpServletRequest request]
  (-> request-map
      (assoc! :server-port        (.getServerPort request))
      (assoc! :server-name        (.getServerName request))
      (assoc! :remote-addr        (.getRemoteAddr request))
      (assoc! :uri                (.getRequestURI request))
      (assoc! :query-string       (.getQueryString request))
      (assoc! :scheme             (keyword (.getScheme request)))
      (assoc! :request-method     (get-request-method request))
      (assoc! :protocol           (.getProtocol request))
      (assoc! :headers            (build-header-map-1 request))
      (assoc! :content-type       (.getContentType request))
      (assoc! :content-length     (get-content-length request))
      (assoc! :character-encoding (.getCharacterEncoding request))
      (assoc! :ssl-client-cert    (get-client-cert request))
      (assoc! :body               (.getInputStream request))))

(defn- assoc-request-fields-2! [request-map ^HttpServletRequest request]
  (let [query (.getQueryString request)
        cert  (get-client-cert request)]
    (-> request-map
        (assoc! ::req/server-port (.getServerPort request))
        (assoc! ::req/server-name (.getServerName request))
        (assoc! ::req/remote-addr (.getRemoteAddr request))
        (assoc! ::req/path        (.getRequestURI request))
        (assoc! ::req/scheme      (keyword (.getScheme request)))
        (assoc! ::req/method      (get-request-method request))
        (assoc! ::req/protocol    (.getProtocol request))
        (assoc! ::req/headers     (build-header-map-2 request))
        (assoc! ::req/body        (.getInputStream request))
        (cond-> (not (string/blank? query))
          (assoc! ::req/query query))
        (cond-> (some? cert)
          (assoc! ::req/ssl-client-cert cert)))))

(defn build-request-map
  "Create a Ring request map from a HttpServletRequest object. Includes keys
  for both Ring 1 and Ring 2."
  [request]
  (-> (transient {})
      (assoc-request-fields-1! request)
      (assoc-request-fields-2! request)
      persistent!))

(defn build-request-map-1
  "Create a Ring request map from a HttpServletRequest object. Includes keys
  for *only* Ring 1."
  [request]
  (-> (transient {})
      (assoc-request-fields-1! request)
      persistent!))

(defn build-request-map-2
  "Create a Ring request map from a HttpServletRequest object. Includes keys
  for *only* Ring 2."
  [^HttpServletRequest request]
  (-> (transient {})
      (assoc-request-fields-2! request)
      persistent!))

(defn- assoc-servlet-keys-1!
  [request-map
   ^HttpServlet servlet
   ^HttpServletRequest request
   ^HttpServletResponse response]
  (-> request-map
      (assoc! :servlet              servlet)
      (assoc! :servlet-request      request)
      (assoc! :servlet-response     response)
      (assoc! :servlet-context      (.getServletContext servlet))
      (assoc! :servlet-context-path (.getContextPath request))))

(defn- assoc-servlet-keys-2!
  [request-map
   ^HttpServlet servlet
   ^HttpServletRequest request
   ^HttpServletResponse response]
  (-> request-map
      (assoc! :ring.servlet/servlet     servlet)
      (assoc! :ring.servlet/request     request)
      (assoc! :ring.servlet/response    response)
      (assoc! :ring.servlet/context     (.getServletContext servlet))
      (assoc! :ring.servle/context-path (.getContextPath request))))

(defn merge-servlet-keys
  "Associate servlet-specific keys with a request map for use with legacy
  systems. Includes keys for both Ring 1 and Ring 2."
  [request-map servlet request response]
  (-> (transient request-map)
      (assoc-servlet-keys-1! servlet request response)
      (assoc-servlet-keys-2! servlet request response)
      persistent!))

(defn merge-servlet-keys-1
  "Associate servlet-specific keys with a request map for use with legacy
  systems. Includes keys for *only* Ring 1."
  [request-map servlet request response]
  (-> (transient request-map)
      (assoc-servlet-keys-1! servlet request response)
      persistent!))

(defn merge-servlet-keys-2
  "Associate servlet-specific keys with a request map for use with legacy
  systems. Includes keys for *only* Ring 2."
  [request-map servlet request response]
  (-> (transient request-map)
      (assoc-servlet-keys-2! servlet request response)
      persistent!))

(defn- set-headers [^HttpServletResponse response, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val)))
    ; Some headers must be set through specific methods
    (when (.equalsIgnoreCase ^String key "content-type")
      (.setContentType response (if (string? val-or-vals)
                                  val-or-vals
                                  (first val-or-vals))))))

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
     (when-let [content-type (first (get headers "content-type"))]
       (.setContentType response content-type))
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

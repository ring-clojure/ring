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
     (let [key (.toLowerCase name Locale/ENGLISH)]
       (assoc headers
              key
              (->> (.getHeaders request name)
                   (enumeration-seq)
                   (string/join (if (= key "cookie") ";" ","))))))
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
  "Create a Ring request map from a HttpServletRequest object. Includes keys
  for both Ring 1 and Ring 2."
  [^HttpServletRequest request]
  (let [server-port (.getServerPort request)
        server-name (.getServerName request)
        remote-addr (.getRemoteAddr request)
        path        (.getRequestURI request)
        query       (.getQueryString request)
        scheme      (keyword (.getScheme request))
        method      (get-request-method request)
        protocol    (.getProtocol request)
        headers     (build-header-map request)
        cert        (get-client-cert request)
        body        (.getInputStream request)]
    (-> {:server-port        server-port
         ::req/server-port   server-port
         :server-name        server-name
         ::req/server-name   server-name
         :remote-addr        remote-addr
         ::req/remote-addr   remote-addr
         :uri                path
         ::req/path          path
         :scheme             scheme
         ::req/scheme        scheme
         :request-method     method
         ::req/method        method
         :protocol           protocol
         ::req/protocol      protocol
         :headers            headers
         ::req/headers       headers
         :body               body
         ::req/body          body
         :query-string       query
         :ssl-client-cert    cert
         :content-type       (.getContentType request)
         :content-length     (get-content-length request)
         :character-encoding (.getCharacterEncoding request)}
        (cond-> (not (string/blank? query))
          (assoc ::req/query query))
        (cond-> (some? cert)
          (assoc ::req/ssl-client-cert cert)))))

(defn build-request-map-1
  "Create a Ring request map from a HttpServletRequest object. Includes keys
  for *only* Ring 1."
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

(defn build-request-map-2
  "Create a Ring request map from a HttpServletRequest object. Includes keys
  for *only* Ring 2."
  [^HttpServletRequest request]
  (let [query (.getQueryString request)
        cert  (get-client-cert request)]
    (-> #::req{:server-port (.getServerPort request)
               :server-name (.getServerName request)
               :remote-addr (.getRemoteAddr request)
               :path        (.getRequestURI request)
               :scheme      (keyword (.getScheme request))
               :method      (get-request-method request)
               :protocol    (.getProtocol request)
               :headers     (build-header-map request)
               :body        (.getInputStream request)}
        (cond-> (not (string/blank? query))
          (assoc ::req/query query))
        (cond-> (some? cert)
          (assoc ::req/ssl-client-cert cert)))))

(defn merge-servlet-keys-1
  "Associate servlet-specific keys with a request map for use with legacy
  systems. Includes keys for *only* Ring 1."
  [request-map ^HttpServlet servlet ^HttpServletRequest request response]
  (merge request-map
         {:servlet              servlet
          :servlet-request      request
          :servlet-response     response
          :servlet-context      (.getServletContext servlet)
          :servlet-context-path (.getContextPath request)}))

(defn merge-servlet-keys-2
  "Associate servlet-specific keys with a request map for use with legacy
  systems. Includes keys for *only* Ring 2."
  [request-map ^HttpServlet servlet ^HttpServletRequest request response]
  (merge request-map
         #:ring.servlet{:servlet      servlet
                        :request      request
                        :response     response
                        :context      (.getServletContext servlet)
                        :context-path (.getContextPath request)}))

(defn merge-servlet-keys
  "Associate servlet-specific keys with a request map for use with legacy
  systems. Includes keys for both Ring 1 and Ring 2."
  [request-map servlet request response]
  (-> request-map
      (merge-servlet-keys-1 servlet request response)
      (merge-servlet-keys-2 servlet request response)))

(defn- validate-response [response response-map]
  (when (nil? response)
    (throw (NullPointerException. "HttpServletResponse is nil")))
  (when (nil? response-map)
    (throw (NullPointerException. "Response map is nil"))))

(defn- set-headers-1 [^HttpServletResponse response, headers]
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

(defn- set-headers-2 [^HttpServletResponse response, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val))))
  ; Some headers must be set through specific methods
  (when-let [val-or-vals (get headers "content-type")]
    (.setContentType response (if (string? val-or-vals)
                                val-or-vals
                                (first val-or-vals)))))

(defn- make-output-stream [^HttpServletResponse response ^AsyncContext context]
  (let [os (.getOutputStream response)]
    (if (nil? context)
      os
      (proxy [java.io.FilterOutputStream] [os]
        (close []
          (.close os)
          (.complete context))))))

(defn update-servlet-response-1
  "Update the HttpServletResponse using a Ring 1 response map. Takes an
  optional AsyncContext."
  ([response response-map]
   (update-servlet-response-1 response nil response-map))
  ([^HttpServletResponse response context response-map]
   (validate-response response response-map)
   (let [{:keys [status headers body]} response-map]
     (when status
       (.setStatus response status))
     (set-headers-1 response headers)
     (let [output-stream (make-output-stream response context)]
       (protocols/write-body-to-stream body response-map output-stream)))))

(defn update-servlet-response-2
  "Update the HttpServletResponse using a Ring 2 response map. Takes an
  optional AsyncContext."
  ([response response-map]
   (update-servlet-response-2 response nil response-map))
  ([^HttpServletResponse response context response-map]
   (validate-response response response-map)
   (let [{:ring.response/keys [status headers body]} response-map]
     (.setStatus response status)
     (set-headers-2 response headers)
     (let [output-stream (make-output-stream response context)]
       (resp/write-body-to-stream response-map output-stream)))))

(defn update-servlet-response
  "Update the HttpServletResponse using a Ring 1 *or* Ring 2 response map.
  Takes an optional AsyncContext."
  ([response response-map]
   (update-servlet-response response nil response-map))
  ([response context response-map]
   (if (contains? response-map ::resp/status)
     (update-servlet-response-2 response context response-map)
     (update-servlet-response-1 response context response-map))))

(defn request-response-functions
  "Given a Ring version (1 or 2), return a map of three functions,
  `:build-request-map`, `:update-servlet-response` and `merge-servlet-keys`
  that will build the request map, update the servlet response and add optional
  servlet keys.

  See: [[build-request-map]], [[update-servlet-response]] and
  [[merge-servlet-keys]]."
  ([]
   (request-response-functions nil))
  ([ring-version]
   (case ring-version
     1   {:build-request-map       build-request-map-1
          :update-servlet-response update-servlet-response-1
          :merge-servlet-keys      merge-servlet-keys-1}
     2   {:build-request-map       build-request-map-2
          :update-servlet-response update-servlet-response-2
          :merge-servlet-keys      merge-servlet-keys-2}
     nil {:build-request-map       build-request-map
          :update-servlet-response update-servlet-response
          :merge-servlet-keys      merge-servlet-keys})))

(defn- make-blocking-service-method
  [handler {:keys [build-request-map
                   update-servlet-response
                   merge-servlet-keys]}]
  (fn [servlet request response]
    (-> request
        (build-request-map)
        (merge-servlet-keys servlet request response)
        (handler)
        (->> (update-servlet-response response)))))

(defn- make-async-service-method
  [handler {:keys [build-request-map
                   update-servlet-response
                   merge-servlet-keys]}]
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

(defn make-service-method
  "Turns a handler into a function that takes the same arguments and has the
  same return value as the service method in the HttpServlet class."
  ([handler]
   (make-service-method handler {}))
  ([handler options]
   (let [req-resp-fns (request-response-functions (:ring options))]
     (if (:async? options)
       (make-async-service-method handler req-resp-fns)
       (make-blocking-service-method handler req-resp-fns)))))

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

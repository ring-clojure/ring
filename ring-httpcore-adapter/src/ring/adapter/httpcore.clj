(ns ring.adapter.httpcore
  "Adapter for the Apache HttpCore webserver."
  (:import (org.apache.http
             HttpRequest Header HttpEntityEnclosingRequest HttpResponse
             ConnectionClosedException HttpException HttpServerConnection)
           (org.apache.http.entity
             AbstractHttpEntity StringEntity EntityTemplate InputStreamEntity
             FileEntity ContentProducer)
           (org.apache.http.message
             BasicHeader BasicHeaderElement)
           (org.apache.http.params
             BasicHttpParams CoreConnectionPNames CoreProtocolPNames)
           (org.apache.http.protocol
             HttpRequestHandler BasicHttpContext HttpService BasicHttpProcessor
             ResponseDate ResponseServer ResponseContent ResponseConnControl
             HttpRequestHandlerRegistry HttpContext)
           (org.apache.http.impl
             DefaultConnectionReuseStrategy DefaultHttpResponseFactory
             DefaultHttpServerConnection)
           (java.io
             File FileInputStream InputStream OutputStream OutputStreamWriter
             IOException InterruptedIOException)
           (java.net URI
             ServerSocket)
           (java.util.concurrent
             Executors Executor ThreadFactory))
  (:use [clojure.contrib.except :only (throwf)]))

(defmacro ^{:private true} -?>
 ([form] form)
 ([form next-form & forms]
   `(when-let [x# ~form] (-?> (-> x# ~next-form) ~@forms))))

(defmacro ^{:private true} instance?-> [type x & forms]
  `(when (instance? ~type ~x) (-> ~(vary-meta x assoc :tag type) ~@forms)))

(defn- charset [^BasicHeader content-type-header]
  (-?> content-type-header .getElements
    ^BasicHeaderElement first (.getParameterByName "charset") .getValue))

(defn- lower-case [^String s]
  (.toLowerCase s java.util.Locale/ENGLISH))

(defn- build-req-map
  "Augments the given request-prototype (a map) to represent the given HTTP
  request, to be passed as the Ring request to a handler."
  [^HttpRequest request request-prototype]
  (let [request-line (.getRequestLine request)
        headers (reduce
                  (fn [header-map ^Header header]
                    (assoc header-map
                      (-> header .getName lower-case)
                      (.getValue header)))
                  {} (seq (.getAllHeaders request)))
        host (or (headers "host")
               (str (request-prototype :server-name) ":"
                    (request-prototype :server-port 80)))
        uri (URI. (str "http://" host (.getUri request-line)))]
    (into (or request-prototype {})
      {:server-port        (.getPort uri)
       :server-name        (.getHost uri)
       :uri                (.getRawPath uri)
       :query-string       (.getRawQuery uri)
       :request-method     (-> request-line .getMethod lower-case keyword)
       :headers            headers
       :content-type       (headers "content-type")
       :content-length     (when-let [len (instance?-> HttpEntityEnclosingRequest request .getEntity .getContentLength)]
                             (when (>= len 0) len))
       :character-encoding (instance?-> HttpEntityEnclosingRequest
                             request .getEntity .getContentEncoding charset)
       :body               (instance?-> HttpEntityEnclosingRequest
                             request .getEntity .getContent)})))

(defn- apply-resp-map
  "Apply the given response map to the servlet response, therby completing
  the HTTP response."
  [^HttpResponse response {:keys [status headers body]}]
  ; Apply the status.
  (.setStatusCode response status)
  ; Apply the headers.
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val))))
  ; Apply the body - the method depends on the given body type.
  (when body
    (let [content-type (headers "Content-Type")
          charset (when content-type
                    (charset (BasicHeader. "Content-Type" content-type)))
          content-length (headers "Content-Length")
          entity
           (cond
             (string? body)
               (StringEntity. body)
             (seq? body)
               (EntityTemplate.
                 (reify ContentProducer
                        (writeTo [this ^OutputStream s]
                                 (let [w (if charset
                                           (OutputStreamWriter. s ^String charset)
                                           (OutputStreamWriter. s))]
                                   (doseq [^String chunk body]
                                     (.write w chunk))
                                   (.flush w)))))
             (instance? InputStream body)
               (InputStreamEntity. body
                 (let [l (or content-length -1)]
                   (if (>= Long/MAX_VALUE l) l -1)))
             (instance? File body)
               (FileEntity. body content-type)
             :else
               (throwf "Unrecognized body: %s" body))]
      (when-let [^String type (headers "Content-Type")]
        (.setContentType ^AbstractHttpEntity entity type))
      (.setEntity response entity))))

(defn- ring-handler
  "Returns an Handler implementation for the given Ring handler.
   The HttpContext must contains a map associated to \"ring.request-prototype\"."
  [handler]
  (reify HttpRequestHandler
      (handle [this request response ^HttpContext context]
        (let [req (build-req-map request
                                 (.getAttribute context "ring.request-prototype"))
              resp (handler req)]
          (apply-resp-map response resp)))))

(defn- handle-request
 "Handle the request, usually called from a worker thread."
 [^HttpService httpservice ^HttpServerConnection conn request-prototype]
  (let [context (doto (BasicHttpContext. nil)
                  (.setAttribute "ring.request-prototype" request-prototype))]
    (try
      (while (.isOpen conn)
        (.handleRequest httpservice conn context))
      (catch ConnectionClosedException _ nil)
      (catch IOException _ nil)
      (catch HttpException _ nil)
      (finally
        (try
          (.shutdown conn)
          (catch IOException _ nil))))))

(defn- create-http-service [handler]
  (let [params (doto (BasicHttpParams.)
                 (.setParameter CoreProtocolPNames/ORIGIN_SERVER
                   "HttpComponents/1.1"))
        httpproc (doto (BasicHttpProcessor.)
                   (.addInterceptor (ResponseDate.))
                   (.addInterceptor (ResponseServer.))
                   (.addInterceptor (ResponseContent.))
                   (.addInterceptor (ResponseConnControl.)))
        registry (doto (HttpRequestHandlerRegistry.)
                   (.register "*" (ring-handler handler)))]
    (doto (HttpService. httpproc
            (DefaultConnectionReuseStrategy.)
            (DefaultHttpResponseFactory.))
      (.setParams params)
      (.setHandlerResolver registry))))

(defn executor-execute
 "Executes (apply f args) using the specified Executor."
 [^java.util.concurrent.Executor executor f & args]
  (.execute executor #(apply f args)))

(defn run-httpcore
 "Serve the given handler according to the options.
 Options:
   :port
   :server-name - For old HTTP/1.0 clients
   :server-port - For old HTTP/1.0 clients, when public facing port is different
                  from :port
   :execute     - Function with signature [f & args] that applies f to args,
                  usually in another thread"
 [handler {:keys [port server-name server-port execute]}]
  (let [execute (or execute (partial executor-execute
                              (Executors/newCachedThreadPool
                                (reify ThreadFactory
                                       (newThread [this r]
                                                  (doto (Thread. ^Runnable r)
                                                    (.setDaemon true)))))))
        params (doto (BasicHttpParams.)
                 (.setIntParameter CoreConnectionPNames/SO_TIMEOUT 5000)
                 (.setIntParameter CoreConnectionPNames/SOCKET_BUFFER_SIZE (* 8 1024))
                 (.setBooleanParameter CoreConnectionPNames/STALE_CONNECTION_CHECK false)
                 (.setBooleanParameter CoreConnectionPNames/TCP_NODELAY true))
        httpservice (create-http-service handler)
        request-prototype {:scheme :http :server-port (or server-port port) :server-name server-name}]
    (with-open [serversocket (ServerSocket. port)]
      (while (not (.isInterrupted (Thread/currentThread)))
        (let [socket (.accept serversocket)
              conn (doto (DefaultHttpServerConnection.)
                     (.bind socket params))]
          (execute handle-request httpservice conn
            (assoc request-prototype :remote-addr (-> socket .getInetAddress .getHostAddress))))))))

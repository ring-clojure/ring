(ns ring.adapter.jetty
  (:import (javax.servlet.http HttpServletRequest HttpServletResponse)
           (org.mortbay.jetty.handler AbstractHandler)
           (org.mortbay.jetty Server)
           (java.io File FileInputStream InputStream OutputStream)
           (org.apache.commons.io IOUtils))
  (:use (clojure.contrib fcase except)))

(defn- build-req-map
  "Returns a map representing the given Servlet request, to be passed as the
  Ring request to an app."
  [#^HttpServletRequest request]
  {:server-port        (.getServerPort request)
   :server-name        (.getServerName request)
   :remote-addr        (.getRemoteAddr request)
   :uri                (.getRequestURI request)
   :query-string       (.getQueryString request)
   :scheme             (keyword (.getScheme request))
   :request-method     (keyword (.toLowerCase (.getMethod request)))
   :headers            (reduce
                          (fn [header-map #^String header-name]
                            (assoc header-map
                              (.toLowerCase header-name)
                              (.getHeader request header-name)))
                          {}
                          (enumeration-seq (.getHeaderNames request)))
   :content-type       (.getContentType request)
   :content-length     (let [len (.getContentLength request)]
                         (if (>= len 0) len))
   :character-encoding (.getCharacterEncoding request)
   :body               (.getInputStream request)})

(defn- apply-resp-map
  "Apply the given response map to the servlet response, therby completing
  the HTTP response."
  [#^HttpServletResponse response {:keys [status headers body]}]
  ; Apply the status.
  (.setStatus response status)
  ; Apply the headers.
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val))))
  ; Some headers must be set through specific methods
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType response content-type))
  ; Apply the body - the method depends on the given body type.
  (cond
    (string? body)
      (with-open [writer (.getWriter response)]
        (.println writer body))
    (seq? body)
      (with-open [writer (.getWriter response)]
        (doseq [chunk body]
          (.print writer (str chunk))))
    (instance? InputStream body)
      (let [#^InputStream in body]
        (with-open [out (.getOutputStream response)]
          (IOUtils/copy in out)
          (.close in)
          (.flush out)))
    (instance? File body)
      (let [#^File f body]
        (with-open [fin (FileInputStream. f)]
          (with-open [out (.getOutputStream response)]
            (IOUtils/copy fin out)
            (.flush out))))
    (nil? body)
      nil
    :else
      (throwf "Unreceognized body: %s" body)))

(defn- proxy-handler
  "Returns an Handler implementation for the given app."
  [app]
  (proxy [AbstractHandler] []
    (handle [target request response dispatch]
      (let [req   (build-req-map request)
            resp  (app req)]
        (apply-resp-map response resp)
        (.setHandled request true)))))

(defn run-jetty
  "Serve the given app according to the options.
  Options:
    :port, an Integer."
  [app options]
  (let [port    (or (:port options) (throwf ":port missing from options"))
        server  (doto (Server. port) (.setSendDateHeader true))
        handler (proxy-handler app)]
    (.setHandler server handler)
    (.start server)
    (.join  server)))

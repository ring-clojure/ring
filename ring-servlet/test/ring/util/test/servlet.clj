(ns ring.util.test.servlet
  (:require [clojure.test :refer :all]
            [ring.util.servlet :refer :all])
  (:import [java.util Locale]))

(defmacro ^:private with-locale [locale & body]
  `(let [old-locale# (Locale/getDefault)]
     (try (Locale/setDefault ~locale)
          (do ~@body)
          (finally (Locale/setDefault old-locale#)))))

(defn- enumeration [coll]
  (let [e (atom coll)]
    (proxy [java.util.Enumeration] []
      (hasMoreElements [] (not (empty? @e)))
      (nextElement [] (let [f (first @e)] (swap! e rest) f)))))

(defn- async-context [completed]
  (proxy [javax.servlet.AsyncContext] []
    (complete [] (reset! completed true))))

(defn- servlet-request [request]
  (let [attributes {"javax.servlet.request.X509Certificate"
                    [(request :ssl-client-cert)]}]
    (proxy [javax.servlet.http.HttpServletRequest] []
      (getServerPort [] (request :server-port))
      (getServerName [] (request :server-name))
      (getRemoteAddr [] (request :remote-addr))
      (getRequestURI [] (request :uri))
      (getQueryString [] (request :query-string))
      (getContextPath [] (request :servlet-context-path))
      (getScheme [] (name (request :scheme)))
      (getMethod [] (-> request :request-method name .toUpperCase))
      (getProtocol [] (request :protocol))
      (getHeaderNames [] (enumeration (keys (request :headers))))
      (getHeaders [name] (enumeration (get-in request [:headers name])))
      (getContentType [] (request :content-type))
      (getContentLength [] (or (request :content-length) -1))
      (getCharacterEncoding [] (request :character-encoding))
      (getAttribute [k] (attributes k))
      (getInputStream [] (request :body))
      (startAsync [] (async-context (request :completed))))))

(defn- servlet-response [response]
  (let [output-stream (java.io.ByteArrayOutputStream.)]
    (swap! response assoc :body output-stream)
    (proxy [javax.servlet.http.HttpServletResponse] []
      (getOutputStream []
        (proxy [javax.servlet.ServletOutputStream] []
          (write
            ([b] (.write output-stream b))
            ([b off len] (.write output-stream b off len)))))
      (setStatus [status]
        (swap! response assoc :status status))
      (setHeader [name value]
        (swap! response assoc-in [:headers name] value))
      (setCharacterEncoding [value])
      (setContentType [value]
        (swap! response assoc :content-type value)))))

(defn- servlet-config []
  (proxy [javax.servlet.ServletConfig] []
    (getServletContext [] nil)))

(defn- run-servlet
  ([handler request response]
   (run-servlet handler request response {}))
  ([handler request response options]
   (doto (servlet handler options)
     (.init (servlet-config))
     (.service (servlet-request request)
               (servlet-response response)))))

(deftest make-service-method-test
  (let [handler (constantly {:status  201
                             :headers {}})
        method  (make-service-method handler)
        servlet (doto (proxy [javax.servlet.http.HttpServlet] [])
                  (.init (servlet-config)))
        request {:server-port    8080
                 :server-name    "foobar"
                 :remote-addr    "127.0.0.1"
                 :uri            "/foo"
                 :scheme         :http
                 :request-method :get
                 :protocol       "HTTP/1.1"
                 :headers        {}}
        response (atom {})]
    (method servlet (servlet-request request) (servlet-response response))
    (is (= (@response :status) 201))))

(deftest servlet-test
  (let [body (proxy [javax.servlet.ServletInputStream] [])
        cert (proxy [java.security.cert.X509Certificate] [])
        request {:server-port    8080
                 :server-name    "foobar"
                 :remote-addr    "127.0.0.1"
                 :uri            "/foo"
                 :query-string   "a=b"
                 :scheme         :http
                 :request-method :get
                 :protocol       "HTTP/1.1"
                 :headers        {"X-Client" ["Foo", "Bar"]
                                  "X-Server" ["Baz"]
                                  "X-Capital-I" ["Qux"]}
                 :content-type   "text/plain"
                 :content-length 10
                 :character-encoding "UTF-8"
                 :servlet-context-path "/foo"
                 :ssl-client-cert cert
                 :body            body}
          response (atom {})]
    (letfn [(handler [r]
             (are [k v] (= (r k) v)
               :server-port    8080
               :server-name    "foobar"
               :remote-addr    "127.0.0.1"
               :uri            "/foo"
               :query-string   "a=b"
               :scheme         :http
               :request-method :get
               :protocol       "HTTP/1.1"
               :headers        {"x-client" "Foo,Bar"
                                "x-server" "Baz"
                                "x-capital-i" "Qux"}
               :content-type   "text/plain"
               :content-length 10
               :character-encoding "UTF-8"
               :servlet-context-path "/foo"
               :ssl-client-cert cert
               :body            body)
             {:status 200, :headers {}})]
      (testing "request"
        (run-servlet handler request response))
      (testing "mapping request header names to lower case"
        (with-locale (Locale. "tr")
          (run-servlet handler request response))))
    (testing "response"
      (letfn [(handler [r]
               {:status  200
                :headers {"Content-Type" "text/plain"
                          "X-Server" "Bar"}
                :body    "Hello World"})]
        (run-servlet handler request response)
        (is (= (@response :status) 200))
        (is (= (@response :content-type) "text/plain"))
        (is (= (get-in @response [:headers "X-Server"]) "Bar"))
        (is (= (.toString (@response :body)) "Hello World"))))))

(deftest servlet-cps-test
  (let [handler  (fn [_ respond _]
                   (respond {:status  200
                             :headers {"Content-Type" "text/plain"}
                             :body    "Hello World"}))
        request  {:completed      (atom false)
                  :server-port    8080
                  :server-name    "foobar"
                  :remote-addr    "127.0.0.1"
                  :uri            "/foo"
                  :scheme         :http
                  :request-method :get
                  :protocol       "HTTP/1.1"
                  :headers        {}
                  :body           nil}
        response (atom {})]
    (run-servlet handler request response {:async? true})
    (is (= @(:completed request) true))
    (is (= (@response :status) 200))
    (is (= (@response :content-type) "text/plain"))
    (is (= (.toString (@response :body)) "Hello World"))))

(defn- defservice-test* [service]
  (let [body     (proxy [javax.servlet.ServletInputStream] [])
        servlet  (doto (proxy [javax.servlet.http.HttpServlet] [])
                   (.init (servlet-config)))
        request  {:server-port    8080
                  :server-name    "foobar"
                  :remote-addr    "127.0.0.1"
                  :uri            "/foo"
                  :query-string   ""
                  :scheme         :http
                  :request-method :get
                  :headers        {}
                  :content-type   "text/plain"
                  :content-length 10
                  :character-encoding "UTF-8"
                  :body           body}
        response (atom {})]
    (service servlet
             (servlet-request request)
             (servlet-response response))
    (is (= (@response :status) 200))
    (is (= (get-in @response [:headers "Content-Type" ]) "text/plain"))
    (is (= (.toString (@response :body)) "Hello World"))))

(defn- service-handler [_]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defservice "foo-" service-handler)
(defservice service-handler {})

(deftest defservice-test
  (defservice-test* foo-service)
  (defservice-test* -service))

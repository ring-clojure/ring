(ns ring.util.test.servlet
  (:use clojure.test
        ring.util.servlet
        [ring.util.compat :only (reducible?)])
  (:import [java.io PrintWriter StringWriter]))

(defn- enumeration [coll]
  (let [e (atom coll)]
    (proxy [java.util.Enumeration] []
      (hasMoreElements [] (not (empty? @e)))
      (nextElement [] (let [f (first @e)] (swap! e rest) f)))))

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
      (getHeaderNames [] (enumeration (keys (request :headers))))
      (getHeaders [name] (enumeration (get-in request [:headers name])))
      (getContentType [] (request :content-type))
      (getContentLength [] (or (request :content-length) -1))
      (getCharacterEncoding [] (request :character-encoding))
      (getAttribute [k] (attributes k))
      (getInputStream [] (request :body)))))

(defn- maybe-assoc-body [m]
  (if (contains? m :body)
    m
    (assoc m :body (StringWriter.))))

(defn- servlet-response [response]
  (proxy [javax.servlet.http.HttpServletResponse] []
    (setStatus [status]
      (swap! response assoc :status status))
    (setHeader [name value]
      (swap! response assoc-in [:headers name] value))
    (setCharacterEncoding [value])
    (setContentType [value]
      (swap! response assoc :content-type value))
    (getWriter []
      (-> response (swap! maybe-assoc-body) :body PrintWriter.))))

(defn- servlet-config []
  (proxy [javax.servlet.ServletConfig] []
    (getServletContext [] nil)))

(defn- run-servlet [handler request]
  (let [response (atom {})]
    (doto (servlet handler)
      (.init (servlet-config))
      (.service (servlet-request request)
                (servlet-response response)))
    @response))

(def ^:private request
  (let [body (proxy [javax.servlet.ServletInputStream] [])
        cert (proxy [java.security.cert.X509Certificate] [])]
    {:server-port    8080
     :server-name    "foobar"
     :remote-addr    "127.0.0.1"
     :uri            "/foo"
     :query-string   "a=b"
     :scheme         :http
     :request-method :get
     :headers        {"X-Client" ["Foo", "Bar"]
                      "X-Server" ["Baz"]}
     :content-type   "text/plain"
     :content-length 10
     :character-encoding "UTF-8"
     :servlet-context-path "/foo"
     :ssl-client-cert cert
     :body            body}))

(deftest request-test
  (testing "request"
    (let [handler (fn [r]
                    (are [k v] (= (r k) v)
                         :server-port    8080
                         :server-name    "foobar"
                         :remote-addr    "127.0.0.1"
                         :uri            "/foo"
                         :query-string   "a=b"
                         :scheme         :http
                         :request-method :get
                         :headers        {"x-client" "Foo,Bar"
                                          "x-server" "Baz"}
                         :content-type   "text/plain"
                         :content-length 10
                         :character-encoding "UTF-8"
                         :servlet-context-path "/foo"
                         :ssl-client-cert (:ssl-client-cert request)
                         :body            (:body request))
                    {:status 200, :headers {}})]
      (run-servlet handler request))))

(deftest response-test
  (testing "response"
    (let [handler (fn [r]
                    {:status  200
                     :headers {"Content-Type" "text/plain"
                               "X-Server" "Bar"}
                     :body    nil})
          response (run-servlet handler request)]
      (is (= (response :status) 200))
      (is (= (response :content-type) "text/plain"))
      (is (= (get-in response [:headers "X-Server"]) "Bar")))))

(deftest response-body-test
  (letfn [(mk-handler [body]
            (fn [r]
              {:status 200,
               :headers {"Content-Type" "text/plain"},
               :body body}))
          (run-body [body]
            (-> body mk-handler (run-servlet request) :body str))]
    (is (= (run-body "success") "success") "String body")
    (is (= (run-body (seq ["suc" "cess"])) "success") "ISeq body")
    (if (reducible? [])
      (is (= (run-body ["suc" "cess"]) "success") "CollReduce body"))))

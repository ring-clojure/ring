(ns ring.util.servlet-test
  (:use clojure.test
        ring.util.servlet))

(defn- enumeration [coll]
  (let [e (atom coll)]
    (proxy [java.util.Enumeration] []
      (hasMoreElements [] (not (empty? @e)))
      (nextElement [] (let [f (first @e)] (swap! e rest) f)))))

(defn- servlet-request [request]
  (proxy [javax.servlet.http.HttpServletRequest] []
    (getServerPort [] (request :server-port))
    (getServerName [] (request :server-name))
    (getRemoteAddr [] (request :remote-addr))
    (getRequestURI [] (request :uri))
    (getQueryString [] (request :query-string))
    (getScheme [] (name (request :scheme)))
    (getMethod [] (-> request :request-method name .toUpperCase))
    (getHeaderNames [] (enumeration (keys (request :headers))))
    (getHeader [name] (get-in request [:headers name]))
    (getContentType [] (request :content-type))
    (getContentLength [] (or (request :content-length) -1))
    (getCharacterEncoding [] (request :character-encoding))
    (getInputStream [] (request :body))))

(defn- servlet-response [response]
  (proxy [javax.servlet.http.HttpServletResponse] []
    (setStatus [status]
      (swap! response assoc :status status))
    (setHeader [name value]
      (swap! response assoc-in [:headers name] value))
    (setCharacterEncoding [value]
      (swap! response assoc :character-encoding value))
    (setContentType [value]
      (swap! response assoc :content-type value))))

(defn- servlet-config []
  (proxy [javax.servlet.ServletConfig] []
    (getServletContext [] nil)))

(defn- run-servlet [handler request response]
  (doto (servlet handler)
          (.init (servlet-config))
          (.service (servlet-request request)
                    (servlet-response response))))

(deftest servlet-test
  (let [body (proxy [javax.servlet.ServletInputStream] [])
        request {:server-port    8080
                 :server-name    "foobar"
                 :remote-addr    "127.0.0.1"
                 :uri            "/foo"
                 :query-string   "a=b"
                 :scheme         :http
                 :request-method :get
                 :headers        {"X-Client" "Foo"}
                 :content-type   "text/plain"
                 :content-length 10
                 :character-encoding "UTF-8"
                 :body           body}
          response (atom {})]
    (testing "request"
      (letfn [(handler [r]
               (are [k v] (= (r k) v)
                 :server-port    8080
                 :server-name    "foobar"
                 :remote-addr    "127.0.0.1"
                 :uri            "/foo"
                 :query-string   "a=b"
                 :scheme         :http
                 :request-method :get
                 :headers        {"x-client" "Foo"}
                 :content-type   "text/plain"
                 :content-length 10
                 :character-encoding "UTF-8"
                 :body           body)
               {:status 200, :headers {}})]
        (run-servlet handler request response)))
    (testing "response"
      (letfn [(handler [r]
               {:status  200
                :headers {"Content-Type" "text/plain"
                          "X-Server" "Bar"}
                :body    nil})]
        (run-servlet handler request response)
        (is (= (@response :status) 200))
        (is (= (@response :content-type) "text/plain"))
        (is (= (get-in @response [:headers "X-Server"]) "Bar"))))))

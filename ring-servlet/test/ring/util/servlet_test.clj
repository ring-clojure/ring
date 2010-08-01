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
    (getScheme [] (str (request :scheme)))
    (getMethod [] (-> request :request-method str .toUpperCase))
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
      (swap! response assoc-in [:headers "Content-Type"] value))))

(defn- servlet-config []
  (proxy [javax.servlet.ServletConfig] []
    (getServletContext [] nil)))

(deftest servlet-test
  (testing "request"
    (let [req  {:server-port    8080
                :server-name    "foobar"
                :remote-addr    "127.0.0.1"
                :uri            "/foo"
                :query-string   "a=b"
                :scheme         :http
                :request-method :get
                :headers        {"X-Server" "Foo"}
                :content-type   "text/plain"
                :content-length 10
                :character-encoding "UTF-8"
                :body           nil}
          resp (atom {})
          svlt (servlet (fn [r]
                          (is (= (:uri r) (:uri req)))
                          {:status 200, :headers {}}))]
      (doto svlt
        (.init (servlet-config))
        (.service (servlet-request req)
                  (servlet-response resp))))))

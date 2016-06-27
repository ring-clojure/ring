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
      (getInputStream [] (request :body)))))

(defn- servlet-response [response]
  (proxy [javax.servlet.http.HttpServletResponse] []
    (getOutputStream []
      (proxy [javax.servlet.ServletOutputStream] []
        (write [body & _]
          (swap! response assoc :body body))))
    (setStatus [status]
      (swap! response assoc :status status))
    (setHeader [name value]
      (swap! response assoc-in [:headers name] value))
    (setCharacterEncoding [value])
    (setContentType [value]
      (swap! response assoc :content-type value))))

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
        (is (= (take-while (complement zero?) (@response :body))
               (seq (.getBytes "Hello World"))))))))

(deftest servlet-cps-test
  (let [handler  (fn [req cont]
                   (cont {:status  200
                          :headers {"Content-Type" "text/plain"}
                          :body    "Hello World"}))
        request  {:server-port    8080
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
    (is (= (@response :status) 200))
    (is (= (@response :content-type) "text/plain"))
    (is (= (take-while (complement zero?) (@response :body))
           (seq (.getBytes "Hello World"))))))

(defservice "foo-"
  (fn [_]
    {:status  200
     :headers {"Content-Type" "text/plain"}
     :body    "Hello World"}))

(deftest defservice-test
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
    (foo-service servlet
                 (servlet-request request)
                 (servlet-response response))
    (is (= (@response :status) 200))
    (is (= (get-in @response [:headers "Content-Type" ]) "text/plain"))
    (is (= (take-while (complement zero?) (@response :body))
           (seq (.getBytes "Hello World"))))))

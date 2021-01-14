(ns ring.bench.servlet
  (:require [jmh.core :as jmh]
            [ring.util.servlet :as servlet])
  (:import [java.util HashMap ArrayList]
           [javax.servlet AsyncContext]))

(defn http-servlet-request []
  (let [headers (HashMap.
                 {"Content-Type"    (ArrayList. ["application/json"])
                  "Host"            (ArrayList. ["localhost"])
                  "User-Agent"      (ArrayList. ["MockAgent/1.0.0"])
                  "Accept"          (ArrayList. ["application/json" "*/*"])
                  "Accept-Encoding" (ArrayList. ["gzip, deflate"])
                  "Connection"      (ArrayList. ["keep-alive"])
                  "Content-Length"  (ArrayList. ["14"])})]
    (reify javax.servlet.http.HttpServletRequest
      (getServerPort        [_] 8080)
      (getServerName        [_] "localhost")
      (getRemoteAddr        [_] "localhost")
      (getRequestURI        [_] "/example")
      (getQueryString       [_] "q=test")
      (getScheme            [_] "http")
      (getMethod            [_] "GET")
      (getProtocol          [_] "HTTP/1.1")
      (getHeaderNames       [_]   (java.util.Collections/enumeration (.keySet headers)))
      (getHeaders           [_ k] (java.util.Collections/enumeration (.get headers k)))
      (getAttribute         [_ _] nil)
      (getContentType       [_] "application/json")
      (getContentLength     [_] 14)
      (getCharacterEncoding [_] nil)
      (getInputStream       [_] (proxy [javax.servlet.ServletInputStream] [])))))

(defn http-servlet-response []
  (reify javax.servlet.http.HttpServletResponse
    (setStatus [_ _])
    (setHeader [_ _ _])
    (addHeader [_ _ _])
    (setContentType [_ _])
    (getOutputStream [_]
      (let [os (java.io.ByteArrayOutputStream.)]
        (proxy [javax.servlet.ServletOutputStream] []
          (close [] (.close os))
          (flush [] (.flush os))
          (write
            ([b] (.write os ^int b))
            ([b off len] (.write os b off len))))))))

(defn async-context []
  (reify AsyncContext
    (complete [_])))

(defn ring-response [body-size]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    (apply str (repeat body-size "x"))})

(defn servlet-handler [servlet-request servlet-response response]
  (let [handler (fn [_] response)]
    (->> servlet-request
         servlet/build-request-map
         handler
         (servlet/update-servlet-response servlet-response))))

(def bench-env
  {:benchmarks
   [{:name :build,        :fn `servlet/build-request-map,       :args [:state/servlet-request]}
    {:name :update,       :fn `servlet/update-servlet-response, :args [:state/servlet-response :state/response]}
    {:name :update-async, :fn `servlet/update-servlet-response, :args [:state/servlet-response :state/async-context :state/response]}
    {:name :handler,      :fn `servlet-handler,                 :args [:state/servlet-request :state/servlet-response :state/response]}]
   :states
   {:servlet-request  {:fn `http-servlet-request, :args []}
    :servlet-response {:fn `http-servlet-response, :args []}
    :response         {:fn `ring-response, :args [:param/response-body-size]}
    :async-context    {:fn `async-context, :args []}}
   :params
   {:response-body-size [128 1024 8192 65536]}})

(def bench-opts
  {:type :quick
   :profilers ["stack"]})

(defn -main []
  (println "Benchmarking...")
  (doseq [result (jmh/run bench-env bench-opts)]
    (println (format "  %-13s - %5s - %.2f ops/s (σ=%.2f)"
                     (:name result)
                     (-> result :params :response_body_size (or "n/a"))
                     (-> result :statistics :mean)
                     (-> result :statistics :stdev)))))

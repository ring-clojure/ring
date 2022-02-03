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

(defn ring-1-response [body-size]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    (apply str (repeat body-size "x"))})

(defn ring-2-response [body-size]
  #:ring.response{:status  200
                  :headers {"content-type" "application/json"}
                  :body    (apply str (repeat body-size "x"))})

(defn servlet-handler [servlet-request servlet-response response]
  (let [handler (fn [_] response)]
    (->> servlet-request
         servlet/build-request-map
         handler
         (servlet/update-servlet-response servlet-response))))


(defn servlet-handler-1 [servlet-request servlet-response response]
  (let [handler (fn [_] response)]
    (->> servlet-request
         servlet/build-request-map-1
         handler
         (servlet/update-servlet-response-1 servlet-response))))


(defn servlet-handler-2 [servlet-request servlet-response response]
  (let [handler (fn [_] response)]
    (->> servlet-request
         servlet/build-request-map-2
         handler
         (servlet/update-servlet-response-2 servlet-response))))

(def bench-env
  {:benchmarks
   [{:name :build,        :fn `servlet/build-request-map,         :args [:state/servlet-request]}
    {:name :build-1,      :fn `servlet/build-request-map-1,       :args [:state/servlet-request]}
    {:name :build-2       :fn `servlet/build-request-map-2,       :args [:state/servlet-request]}
    {:name :update-1,     :fn `servlet/update-servlet-response,   :args [:state/servlet-response :state/response-1]}
    {:name :update-2,     :fn `servlet/update-servlet-response,   :args [:state/servlet-response :state/response-2]}
    {:name :update-11,    :fn `servlet/update-servlet-response-1, :args [:state/servlet-response :state/response-1]}
    {:name :update-22,    :fn `servlet/update-servlet-response-2, :args [:state/servlet-response :state/response-2]}
    {:name :update-async, :fn `servlet/update-servlet-response,   :args [:state/servlet-response :state/async-context :state/response-1]}
    {:name :handler-1,    :fn `servlet-handler,                   :args [:state/servlet-request :state/servlet-response :state/response-1]}
    {:name :handler-2,    :fn `servlet-handler,                   :args [:state/servlet-request :state/servlet-response :state/response-2]}
    {:name :handler-11,   :fn `servlet-handler-1,                 :args [:state/servlet-request :state/servlet-response :state/response-1]}
    {:name :handler-22,   :fn `servlet-handler-2,                 :args [:state/servlet-request :state/servlet-response :state/response-2]}]
   :states
   {:servlet-request  {:fn `http-servlet-request, :args []}
    :servlet-response {:fn `http-servlet-response, :args []}
    :response-1       {:fn `ring-1-response, :args [:param/response-body-size]}
    :response-2       {:fn `ring-2-response, :args [:param/response-body-size]}
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

(ns ring.bench.servlet
  (:require [clojure.pprint :as pp]
            [jmh.core :as jmh]
            [ring.util.servlet :as servlet])
  (:import [java.util HashMap ArrayList]))

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
            ([b] (.write os b))
            ([b off len] (.write os b off len))))))))

(def ring-1-response
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    "{\"hello\" \"world\"}"})

(def ring-2-response
  #:ring.response{:status  200
                  :headers {"content-type" "application/json"}
                  :body    "{\"hello\" \"world\"}"})

(def bench-env
  {:benchmarks
   [{:name :build-test,   :fn `servlet/build-request-map,   :args [:state/request]}
    {:name :build-test-1, :fn `servlet/build-request-map-1, :args [:state/request]}
    {:name :build-test-2, :fn `servlet/build-request-map-2, :args [:state/request]}
    {:name :update-test-1,  :fn `servlet/update-servlet-response,   :args [:state/response :param/response-1]}
    {:name :update-test-2,  :fn `servlet/update-servlet-response,   :args [:state/response :param/response-2]}
    {:name :update-test-11, :fn `servlet/update-servlet-response-1, :args [:state/response :param/response-1]}
    {:name :update-test-22, :fn `servlet/update-servlet-response-2, :args [:state/response :param/response-2]}]
   :states
   {:request  {:fn `http-servlet-request, :args []}
    :response {:fn `http-servlet-response, :args []}}
   :params
   {:response-1 ring-1-response
    :response-2 ring-2-response}})

(def bench-opts
  {:type :quick
   :profilers ["stack"]})

(defn -main []
  (println "Benchmarking...")
  (doseq [result (jmh/run bench-env bench-opts)]
    (let [[score unit] (:score result)]
      (println (format "  %s - %.2f %s" (:fn result) score unit)))))

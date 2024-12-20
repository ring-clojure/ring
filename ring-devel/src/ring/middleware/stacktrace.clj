(ns ring.middleware.stacktrace
  "Middleware that catches exceptions thrown by the handler, and reports the
  error and stacktrace via a webpage and STDERR log.

  This middleware is for debugging purposes, and should be limited to
  development environments."
  (:require [clojure.java.io :as io]
            [hiccup.core :refer [html h]]
            [hiccup.page :refer [html5]]
            [clj-stacktrace.core :as st]
            [clj-stacktrace.repl :as repl]
            [ring.util.response :refer [content-type response status]]))

(defn wrap-stacktrace-log
  "Wrap a handler such that exceptions are logged to *err* and then rethrown.

  Accepts the following options:

  :color? - if true, apply ANSI colors to stacktrace (default false)"
  ([handler]
   (wrap-stacktrace-log handler {}))
  ([handler options]
   (let [color? (:color? options)]
     (fn
       ([request]
        (try
          (handler request)
          (catch Throwable ex
            (repl/pst-on *err* color? ex)
            (throw ex))))
       ([request respond raise]
        (try
          (handler request respond (fn [ex] (repl/pst-on *err* color? ex) (raise ex)))
          (catch Throwable ex
            (repl/pst-on *err* color? ex)
            (throw ex))))))))

(defn- style-resource [path]
  (html [:style {:type "text/css"} (slurp (io/resource path))]))

(defn- elem-partial [elem]
  (if (:clojure elem)
    [:tr.clojure
     [:td.source (h (repl/source-str elem))]
     [:td.method (h (repl/clojure-method-str elem))]]
    [:tr.java
     [:td.source (h (repl/source-str elem))]
     [:td.method (h (repl/java-method-str elem))]]))

(defn- html-exception [ex]
  (let [[ex & causes] (iterate :cause (st/parse-exception ex))]
    (html5
     [:head
      [:title "Ring: Stacktrace"]
      (style-resource "ring/css/stacktrace.css")]
     [:body
      [:div#exception
       [:h1 (h (.getName ^Class (:class ex)))]
       [:div.message (h (:message ex))]
       [:div.trace
        [:table
         [:tbody (map elem-partial (:trace-elems ex))]]]
       (for [cause causes :while cause]
         [:div#causes
          [:h2 "Caused by " [:span.class (h (.getName ^Class (:class cause)))]]
          [:div.message (h (:message cause))]
          [:div.trace
           [:table
            [:tbody (map elem-partial (:trace-elems cause))]]]])]])))

(defn- text-ex-response [e]
  (-> (response (with-out-str (repl/pst e)))
      (status 500)
      (content-type "text/plain")))

(defn- html-ex-response [ex]
  (-> (response (html-exception ex))
      (status 500)
      (content-type "text/html")))

(defn- ex-response
  "Returns a response showing debugging information about the exception.

  Renders HTML if that's in the accept header (indicating that the URL was
  opened in a browser), but defaults to plain text."
  [req ex]
  (let [accept (get-in req [:headers "accept"])]
    (if (and accept (re-find #"^text/html" accept))
      (html-ex-response ex)
      (text-ex-response ex))))

(defn wrap-stacktrace-web
  "Wrap a handler such that exceptions are caught and a response containing
  a HTML representation of the exception and stacktrace is returned."
  [handler]
  (fn
    ([request]
     (try
       (handler request)
       (catch Throwable ex
         (ex-response request ex))))
    ([request respond _]
     (try
       (handler request respond (fn [ex] (respond (ex-response request ex))))
       (catch Throwable ex
         (respond (ex-response request ex)))))))

(defn wrap-stacktrace
  "Wrap a handler such that exceptions are caught, a corresponding stacktrace is
  logged to *err*, and a HTML representation of the stacktrace is returned as a
  response.

  Accepts the following option:

  :color? - if true, apply ANSI colors to terminal stacktrace (default false)"
  {:arglists '([handler] [handler options])}
  ([handler]
   (wrap-stacktrace handler {}))
  ([handler options]
   (-> handler
       (wrap-stacktrace-log options)
       (wrap-stacktrace-web))))

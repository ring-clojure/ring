(ns ring.middleware.stacktrace
  "Catch exceptions and render web and log stacktraces for debugging."
  (:require [clojure.java.io :as io])
  (:use hiccup.core
        hiccup.page
        clj-stacktrace.core
        clj-stacktrace.repl
        ring.util.response))

(defn wrap-stacktrace-log
  "Wrap a handler such that exceptions are logged to *err* and then rethrown.
  Accepts the following options:
    :color? - apply ANSI colors to stacktrace"
  [handler & [{color? :color?}]]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (pst-on *err* color? ex)
        (throw ex)))))

(defn- style-resource [path]
  (html [:style {:type "text/css"} (slurp (io/resource path))]))

(defn- elem-partial [elem]
  (if (:clojure elem)
    [:tr.clojure
      [:td.source (h (source-str elem))]
      [:td.method (h (clojure-method-str elem))]]
    [:tr.java
      [:td.source (h (source-str elem))]
      [:td.method (h (java-method-str elem))]]))

(defn- html-exception [ex]
  (let [[ex & causes] (iterate :cause (parse-exception ex))]
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

(defn- js-ex-response [e]
  (-> (response (with-out-str (pst e)))
      (status 500)
      (content-type "text/javascript")))

(defn- html-ex-response [ex]
  (-> (response (html-exception ex))
      (status 500)
      (content-type "text/html")))

(defn- ex-response
  "Returns a response showing debugging information about the exception.
  Currently supports delegation to either js or html exception views."
  [req ex]
  (let [accept (get-in req [:headers "accept"])]
    (if (and accept (re-find #"^text/javascript" accept))
      (js-ex-response ex)
      (html-ex-response ex))))

(defn wrap-stacktrace-web
  "Wrap a handler such that exceptions are caught and a helpful debugging
   response is returned."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (ex-response request ex)))))

(defn wrap-stacktrace
  "Wrap a handler such that exceptions are caught, a corresponding stacktrace is
  logged to *err*, and a helpful debugging web response is returned. Accepts the
  following option:
    :color? - apply ANSI colors to terminal stacktrace"
  [handler & [{:as options}]]
  (-> handler
      (wrap-stacktrace-log options)
      (wrap-stacktrace-web)))

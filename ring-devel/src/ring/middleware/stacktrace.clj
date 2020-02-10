(ns ring.middleware.stacktrace
  "Middleware that catches exceptions thrown by the handler, and reports the
  error and stacktrace via a webpage and STDERR log.

  This middleware is for debugging purposes, and should be limited to
  development environments."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.core :refer [html h]]
            [hiccup.page :refer [html5]]
            [clj-stacktrace.core :refer :all]
            [clj-stacktrace.repl :refer :all]
            [ring.util.response :refer [content-type response status]]))

(defn- swap-trace-elems
  "Recursively replace :trace-elems with :trimmed-elems"
  [exception]
  (let [trimmed (or (:trimmed-elems exception) '())
        cause   (:cause exception)]
    (if cause
      (assoc exception
             :cause       (swap-trace-elems cause)
             :trace-elems trimmed)
      (assoc exception :trace-elems trimmed))))

(defn- trim-error-elems [ex trimmed?]
  (if trimmed?
    (swap-trace-elems (parse-exception ex))
    (parse-exception ex)))

(defn wrap-stacktrace-log
  "Wrap a handler such that exceptions are logged to *err* and then rethrown.
  Accepts the following options:
  :color?   - if true, apply ANSI colors to stacktrace (default false)
  :trimmed? - if true, use trimmed-elems (default false)"
  ([handler]
   (wrap-stacktrace-log handler {}))
  ([handler options]
   (let [{:keys [color? trimmed?]} options]
     (fn
       ([request]
        (try
          (handler request)
          (catch Throwable ex
            (pst-on *err* color? (trim-error-elems ex trimmed?))
            (throw ex))))
       ([request respond raise]
        (try
          (handler request
                   respond
                   (fn [ex]
                     (pst-on *err*
                             color?
                             (trim-error-elems ex trimmed?))
                     (raise ex)))
          (catch Throwable ex
            (pst-on *err* color? (trim-error-elems ex trimmed?))
            (throw ex))))))))

(defn- style-resource [path]
  (html [:style {:type "text/css"} (slurp (io/resource path))]))

(defn- color-style
  "Returns a style tag with the color appropriate for the given trace elem.
  Cyan is replaced with black for readability on the light background."
  [elem]
  {:style
   {:color (str/replace (name (elem-color elem)) "cyan" "black")}})

(defn- elem-partial [elem color?]
  (if (:clojure elem)
    [:tr.clojure (when color? (color-style elem))
      [:td.source (h (source-str elem))]
      [:td.method (h (clojure-method-str elem))]]
    [:tr.java (when color? (color-style elem))
      [:td.source (h (source-str elem))]
      [:td.method (h (java-method-str elem))]]))

(defn- html-exception [ex color? trimmed?]
  (let [[ex & causes] (iterate :cause (trim-error-elems ex trimmed?))]
    (html5
      [:head
        [:title "Ring: Stacktrace"]
        (style-resource "ring/css/stacktrace.css")]
      [:body
        [:div#exception
          [:h1 (h (.getName ^Class (:class ex)))]
          [:div.message (h (:message ex))]
        (when (pos? (count (:trace-elems ex)))
          [:div.trace
            [:table
              [:tbody (map #(elem-partial % color?) (:trace-elems ex))]]])
         (for [cause causes :while cause]
           [:div#causes
             [:h2 "Caused by " [:span.class (h (.getName ^Class (:class cause)))]]
             [:div.message (h (:message cause))]
             [:div.trace
               [:table
                 [:tbody
                  (map #(elem-partial % color?) (:trace-elems cause))]]]])]])))

(defn- text-ex-response [e]
  (-> (response (with-out-str (pst e)))
      (status 500)
      (content-type "text/plain")))

(defn- html-ex-response [ex color? trimmed?]
  (-> (response (html-exception ex color? trimmed?))
      (status 500)
      (content-type "text/html")))

(defn- ex-response
  "Returns a response showing debugging information about the exception.

  Renders HTML if that's in the accept header (indicating that the URL was
  opened in a browser), but defaults to plain text."
  [req ex color? trimmed?]
  (let [accept (get-in req [:headers "accept"])]
    (if (and accept (re-find #"^text/html" accept))
      (html-ex-response ex color? trimmed?)
      (text-ex-response ex))))

(defn wrap-stacktrace-web
  "Wrap a handler such that exceptions are caught and a response containing
  a HTML representation of the exception and stacktrace is returned.

  Accepts the following option:
  :color?   - if true, apply ANSI colors to HTML stacktrace (default false)
  :trimmed? - if true, use the trimmed-elems (default false)"
  ([handler]
   (wrap-stacktrace-web handler {}))
  ([handler options]
   (let [{:keys [color? trimmed?]} options]
     (fn
       ([request]
        (try
          (handler request)
          (catch Throwable ex
            (ex-response request ex color? trimmed?))))
       ([request respond raise]
        (try
          (handler request
                   respond
                   (fn [ex]
                     (respond (ex-response request ex color? trimmed?))))
          (catch Throwable ex
            (respond (ex-response request ex color? trimmed?)))))))))

(defn wrap-stacktrace
  "Wrap a handler such that exceptions are caught, a corresponding stacktrace is
  logged to *err*, and a HTML representation of the stacktrace is returned as a
  response.

  Accepts the following option:
  :color?   - if true, apply ANSI colors to stacktrace (default false)
  :trimmed? - if true, use the trimmed-elems (default false)"
  {:arglists '([handler] [handler options])}
  ([handler]
   (wrap-stacktrace handler {}))
  ([handler options]
   (-> handler
       (wrap-stacktrace-log options)
       (wrap-stacktrace-web options))))

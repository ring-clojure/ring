(ns ring.middleware.stacktrace
  "Catch exceptions and render web and log stacktraces for debugging."
  (:use hiccup.core
        hiccup.page-helpers
        clj-stacktrace.core
        clj-stacktrace.repl
        ring.util.response))

(defn wrap-stacktrace-log
  "Wrap a handler such that exceptions are logged to *err* and then rethrown."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (let [msg (str "Exception: " (pst-str ex))]
          (.println *err* msg)
          (throw ex))))))

(declare css)

(defn- elem-partial [elem]
  (if (:clojure elem)
    [:tr
      [:td.source (h (source-str elem))]
      [:td.method (h (clojure-method-str elem))]]
    [:tr
      [:td.source (h (source-str elem))]
      [:td.method (h (java-method-str elem))]]))

(defn- html-exception [ex]
  (let [ex-seq    (iterate :cause (parse-exception ex))
        exception (first ex-seq)
        causes    (rest ex-seq)]
    (html5
      [:head
        [:title "Ring: Stacktrace"]
        [:style {:type "text/css"} css]]
      [:body
        [:div#exception
          [:h3.info (h (str ex))]
          [:table.trace
            [:tbody (map elem-partial (:trace-elems exception))]]]
        (for [cause causes :while cause]
          [:div#causes
           [:h3.info "Caused by: "
                    (h (.getName (:class cause))) " "
                    (h (:message cause))]
           [:table.trace
             [:tbody (map elem-partial (:trimmed-elems cause))]]])])))

(defn- js-ex-response [e]
  (-> (response (pst-str e))
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
  logged to *err*, and a helpful debugging web response is returned."
  [handler]
  (-> handler
      wrap-stacktrace-log
      wrap-stacktrace-web))

(def ^{:private true} css "
/*
Copyright (c) 2008, Yahoo! Inc. All rights reserved.
Code licensed under the BSD License:
http://developer.yahoo.net/yui/license.txt
version: 2.6.0
*/
html{color:#000;background:#FFF;}body,div,dl,dt,dd,ul,ol,li,h1,h2,h3,h4,h5,h6,pre,code,form,fieldset,legend,input,textarea,p,blockquote,th,td{margin:0;padding:0;}table{border-collapse:collapse;border-spacing:0;}fieldset,img{border:0;}address,caption,cite,code,dfn,em,strong,th,var{font-style:normal;font-weight:normal;}li{list-style:none;}caption,th{text-align:left;}h1,h2,h3,h4,h5,h6{font-size:100%;font-weight:normal;}q:before,q:after{content:'';}abbr,acronym{border:0;font-variant:normal;}sup{vertical-align:text-top;}sub{vertical-align:text-bottom;}input,textarea,select{font-family:inherit;font-size:inherit;font-weight:inherit;}input,textarea,select{*font-size:100%;}legend{color:#000;}del,ins{text-decoration:none;}

h3.info {
 font-size: 1.6em; 
 margin-left: 1em;
 padding-top: .5em;
 padding-bottom: .5em;
}

table.trace {
  font-size: 1.1em;
  margin-left: 1em;
  background: lightgrey;
}

table.trace tr {
  line-height: 1.4em;
}

table.trace td.method {
  padding-left: .5em;
  text-aligh: left;
}

table.trace td.source {
  text-align: right;
}")

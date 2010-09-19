(ns ring.middleware.stacktrace
  "Catch exceptions and render web and log stacktraces for debugging."
  (:use (hiccup core page-helpers)
        (clj-stacktrace core repl)
        (clojure.contrib [def :only (defvar-)])
        ring.util.response))

(defn wrap-stacktrace-log
  "Wrap an app such that exceptions are logged to *err* and then rethrown."
  [app]
  (fn [req]
    (try
      (app req)
      (catch Exception e
        (let [msg (str "Exception: " (pst-str e))]
          (.println *err* msg)
          (throw e))))))

(declare css)

(defn- js-ex-response [e]
  (-> (response (pst-str e))
    (status 500)
    (content-type "text/javascript")))

(defn- elem-partial [elem]
  (if (:clojure elem)
    [:tr
      [:td.source (h (source-str         elem))]
      [:td.method (h (clojure-method-str elem))]]
    [:tr
      [:td.source (h (source-str      elem))]
      [:td.method (h (java-method-str elem))]]))

(defn- html-ex-view [e]
  (let [e-parsed (parse-exception e)]
    (html
      (doctype :xhtml-transitional)
      [:html {:xmlns "http://www.w3.org/1999/xhtml"}
        [:head
          [:meta {:http-equiv "Content-Type" :content "text/html;charset=utf-8"}]
          [:title "Ring: Stacktrace"]
          [:style {:type "text/css"} css]
          [:body
            [:div#content
              [:h3.info (h (str e))]
              [:table.trace [:tbody
                (map elem-partial (:trace-elems e-parsed))]]]]]])))

(defn- html-ex-response [e]
  (-> (response (html-ex-view e))
    (status 500)
    (content-type "text/html")))

(defn- ex-response
  "Returns a response showing debugging information about the exception.
  Currently supports delegation to either js or html exception views."
  [req e]
  (let [accept (get-in req [:headers "accept"])]
    (if (and accept (re-find #"^text/javascript" accept))
      (js-ex-response e)
      (html-ex-response e))))

(defn wrap-stacktrace-web
  "Wrap an app such that exceptions are caught and a helpful debugging response
   is returned."
  [app]
  (fn [req]
    (try
      (app req)
      (catch Exception e
        (ex-response req e)))))

(defn wrap-stacktrace
  "Wrap an app such that exceptions are caught, a corresponding stacktrace is
  logged to *err*, and a helpful debugging web response is returned."
  [app]
  (-> app
    wrap-stacktrace-log
    wrap-stacktrace-web))

(defvar- css "
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

(ns ring.dump
  (:use (clj-html core utils helpers)
        clojure.contrib.def
        clojure.set)
  (:import (org.apache.commons.io IOUtils)))

(declare css)

(def ring-keys
  '(:server-port :server-name :remote-addr :uri :query-string :scheme
    :request-method :content-type :content-length :character-encoding
    :headers :body))

(defhtml req-pair
  [key req]
  [:tr [:td.key  (h (str key))]
       [:td.val  (h (pr-str (key req)))]])

(defhtml template
  [req]
  (doctype :xhtml-transitional)
  [:html {:xmlns "http://www.w3.org/1999/xhtml"}
    [:head
      [:meta {:http-equiv "Content-Type" :content "text/html"}]
      [:title "Ring: Request Dump"]]
      [:style {:type "text/css"} css]
    [:body
      [:div#content
        [:h3.info "Ring Request Values"]
        [:table.request
          [:tbody
            (domap-str [key ring-keys]
              (req-pair key req))]]
        (if-let [user-keys (difference (set (keys req)) (set ring-keys))]
          (html
             [:br]
             [:table.request.user
               [:tbody [:tr
                 (domap-str [key (sort user-keys)]
                   (req-pair key req))]]]))]]])

(defn app
  "Returns a response tuple corresponding to an HTML dump of the request
  req as it was recieved by this app."
  [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (template req)})

(def css "
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

table.request {
  font-size: 1.1em;
  width: 800px;
  margin-left: 1em;
  margin-right: 1em;
  background: lightgrey;
}

table.request tr {
  line-height: 1.4em;
}

table.request td.key {
  padding-left: .5em;
  text-aligh: left;
  width: 150px;
}

table.request td.val {
  text-align: left;
}")

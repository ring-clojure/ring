(ns ring.handler.dump
  "A handler that displays the received request map.

  This is useful for debugging new adapters."
  (:require [clojure.set :as set]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]
            [hiccup.core :refer [html h]]
            [hiccup.page :refer [doctype]]
            [hiccup.def :refer [defhtml]]
            [ring.util.response :refer [content-type response]]))

(def ^:no-doc ring-keys
  '(:server-port :server-name :remote-addr :uri :query-string :scheme
    :request-method :content-type :content-length :character-encoding
    :ssl-client-cert :headers :body))

(defn- style-resource [path]
  (html [:style {:type "text/css"} (slurp (io/resource path))]))

(defn- req-pair [key req]
  (html
    [:tr
      [:td.key  (h (str key))]
      [:td.val  (h (pr-str (key req)))]]))

(defhtml ^:no-doc template
  [req]
  (doctype :xhtml-transitional)
  [:html {:xmlns "http://www.w3.org/1999/xhtml"}
    [:head
      [:title "Ring: Request Dump"]
      (style-resource "ring/css/dump.css")]
    [:body
      [:div#content
        [:h3.info "Ring Request Values"]
        [:table.request
          [:tbody
            (for [key ring-keys]
              (req-pair key req))]]
        (if-let [user-keys (set/difference (set (keys req)) (set ring-keys))]
          (html
            [:br]
            [:table.request.user
              [:tbody [:tr
                (for [key (sort user-keys)]
                  (req-pair key req))]]]))]]])

(defn handle-dump
  "Returns a HTML response that shows the information in the request map.
  Also prints the request map to STDOUT."
  ([request]
   (pprint/pprint request)
   (println)
   (-> (response (template request))
       (content-type "text/html")))
  ([request respond raise]
   (respond (handle-dump request))))

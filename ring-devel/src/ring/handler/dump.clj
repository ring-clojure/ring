(ns ring.handler.dump
  "Reflect Ring requests into responses for debugging."
  (:use hiccup.core
        hiccup.page
        hiccup.def
        ring.util.response)
  (:require [clojure.set :as set]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]))

(declare css)

(def ring-keys
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

(defhtml template
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
  "Returns a response tuple corresponding to an HTML dump of the request
  req as it was received by this app."
  [req]
  (pprint/pprint req)
  (println)
  (-> (response (template req))
      (content-type "text/html")))

(ns ring.middleware.cookies
  "Cookie manipulation."
  (:require [ring.util.codec :as codec]
            [clojure.string :as str])
  (:use [clj-time.core :only (in-secs)]
        [clj-time.format :only (formatters unparse)]
        [ring.util.parsing :only (re-token re-value)])
  (:import (org.joda.time Interval DateTime)))

(def ^{:private true, :doc "RFC6265 cookie-octet"}
  re-cookie-octet
  #"[!#$%&'()*+\-./0-9:<=>?@A-Z\[\]\^_`a-z\{\|\}~]")

(def ^{:private true, :doc "RFC6265 cookie-value"}
  re-cookie-value
  (re-pattern (str "\"" re-cookie-octet "*\"|" re-cookie-octet "*")))

(def ^{:private true, :doc "RFC6265 set-cookie-string"}
  re-cookie
  (re-pattern (str "\\s*(" re-token ")=(" re-cookie-value ")\\s*[;,]?")))

(def ^{:private true
       :doc "Attributes defined by RFC6265 that apply to the Set-Cookie header."}
  set-cookie-attrs
  {:domain "Domain", :max-age "Max-Age", :path "Path"
   :secure "Secure", :expires "Expires", :http-only "HttpOnly"})

(defn- parse-cookie-header
  "Turn a HTTP Cookie header into a list of name/value pairs."
  [header]
  (for [[_ name value] (re-seq re-cookie header)]
    [name value]))

(defn- strip-quotes
  "Strip quotes from a cookie value."
  [value]
  (str/replace value #"^\"|\"$" ""))

(defn- decode-values [cookies]
  (for [[name value] cookies]
    (if-let [value (codec/form-decode-str (strip-quotes value))]
      [name {:value value}])))

(defn- parse-cookies
  "Parse the cookies from a request map."
  [request]
  (if-let [cookie (get-in request [:headers "cookie"])]
    (->> cookie
         parse-cookie-header
         decode-values
         (remove nil?)
         (into {}))
    {}))

(defn- write-value
  "Write the main cookie value."
  [key value]
  (codec/form-encode {key value}))

(defn- valid-attr?
  "Is the attribute valid?"
  [[key value]]
  (and (contains? set-cookie-attrs key)
       (not (.contains (str value) ";"))
       (case key
         :max-age (or (instance? Interval value) (integer? value))
         :expires (or (instance? DateTime value) (string? value))
         true)))

(defn- write-attr-map
  "Write a map of cookie attributes to a string."
  [attrs]
  {:pre [(every? valid-attr? attrs)]}
  (for [[key value] attrs]
    (let [attr-name (name (set-cookie-attrs key))]
      (cond
       (instance? Interval value) (str ";" attr-name "=" (in-secs value))
       (instance? DateTime value) (str ";" attr-name "=" (unparse (formatters :rfc822) value))
       (true? value)  (str ";" attr-name)
       (false? value) ""
       :else (str ";" attr-name "=" value)))))

(defn- write-cookies
  "Turn a map of cookies into a seq of strings for a Set-Cookie header."
  [cookies]
  (for [[key value] cookies]
    (if (map? value)
      (apply str (write-value key (:value value))
                 (write-attr-map (dissoc value :value)))
      (write-value key value))))

(defn- set-cookies
  "Add a Set-Cookie header to a response if there is a :cookies key."
  [response]
  (if-let [cookies (:cookies response)]
    (update-in response
               [:headers "Set-Cookie"]
               concat
               (doall (write-cookies cookies)))
    response))

(defn cookies-request
  "Parses cookies in the request map."
  [request]
  (if (request :cookies)
    request
    (assoc request :cookies (parse-cookies request))))

(defn cookies-response
  "For responses with :cookies, adds Set-Cookie header and returns response without :cookies."
  [response]
  (-> response
      (set-cookies)
      (dissoc :cookies)))

(defn wrap-cookies
  "Parses the cookies in the request map, then assocs the resulting map
  to the :cookies key on the request.

  Each cookie is represented as a map, with its value being held in the
  :value key. A cookie may optionally contain a :path, :domain or :port
  attribute.

  To set cookies, add a map to the :cookies key on the response. The values
  of the cookie map can either be strings, or maps containing the following
  keys:

  :value     - the new value of the cookie
  :path      - the subpath the cookie is valid for
  :domain    - the domain the cookie is valid for
  :max-age   - the maximum age in seconds of the cookie
  :expires   - a date string at which the cookie will expire
  :secure    - set to true if the cookie requires HTTPS, prevent HTTP access
  :http-only - set to true if the cookie is valid for HTTP and HTTPS only
               (ie. prevent JavaScript access)"
  [handler]
  (fn [request]
    (-> request
        cookies-request
        handler
        cookies-response)))

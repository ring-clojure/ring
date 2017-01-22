(ns ring.middleware.cookies
  "Middleware for parsing and generating cookies."
  (:import [org.joda.time DateTime Interval])
  (:require [ring.util.codec :as codec]
            [clojure.string :as str]
            [clj-time.core :refer [in-seconds]]
            [clj-time.format :refer [formatters unparse with-locale]]
            [ring.util.parsing :refer [re-token]]))

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
   :secure "Secure", :expires "Expires", :http-only "HttpOnly"
   :same-site "SameSite"})

(def ^{:private true
       :doc "Values defined by RFC6265 that apply to the SameSite cookie attribute header."}
  same-site-values
  {:strict "Strict"
   :lax "Lax"})

(def ^:private rfc822-formatter
  (with-locale (formatters :rfc822) java.util.Locale/US))

(defn- parse-cookie-header
  "Turn a HTTP Cookie header into a list of name/value pairs."
  [header]
  (for [[_ name value] (re-seq re-cookie header)]
    [name value]))

(defn- strip-quotes
  "Strip quotes from a cookie value."
  [value]
  (str/replace value #"^\"|\"$" ""))

(defn- decode-values [cookies decoder]
  (for [[name value] cookies]
    (if-let [value (decoder (strip-quotes value))]
      [name {:value value}])))

(defn- parse-cookies
  "Parse the cookies from a request map."
  [request encoder]
  (if-let [cookie (get-in request [:headers "cookie"])]
    (->> cookie
         parse-cookie-header
         ((fn [c] (decode-values c encoder)))
         (remove nil?)
         (into {}))
    {}))

(defn- write-value
  "Write the main cookie value."
  [key value encoder]
  (encoder {key value}))

(defn- valid-attr?
  "Is the attribute valid?"
  [[key value]]
  (and (contains? set-cookie-attrs key)
       (not (.contains (str value) ";"))
       (case key
         :max-age (or (instance? Interval value) (integer? value))
         :expires (or (instance? DateTime value) (string? value))
         :same-site (contains? same-site-values value)
         true)))

(defn- write-attr-map
  "Write a map of cookie attributes to a string."
  [attrs]
  {:pre [(every? valid-attr? attrs)]}
  (for [[key value] attrs]
    (let [attr-name (name (set-cookie-attrs key))]
      (cond
        (instance? Interval value) (str ";" attr-name "=" (in-seconds value))
        (instance? DateTime value) (str ";" attr-name "=" (unparse rfc822-formatter value))
        (true? value)  (str ";" attr-name)
        (false? value) ""
        (= :same-site key) (str ";" attr-name "=" (same-site-values value))
        :else (str ";" attr-name "=" value)))))

(defn- write-cookies
  "Turn a map of cookies into a seq of strings for a Set-Cookie header."
  [cookies encoder]
  (for [[key value] cookies]
    (if (map? value)
      (apply str (write-value key (:value value) encoder)
                 (write-attr-map (dissoc value :value)))
      (write-value key value encoder))))

(defn- set-cookies
  "Add a Set-Cookie header to a response if there is a :cookies key."
  [response encoder]
  (if-let [cookies (:cookies response)]
    (update-in response
               [:headers "Set-Cookie"]
               concat
               (doall (write-cookies cookies encoder)))
    response))

(defn cookies-request
  "Parses cookies in the request map. See: wrap-cookies."
  {:added "1.2"}
  ([request]
   (cookies-request request {}))
  ([request options]
   (let [{:keys [decoder] :or {decoder codec/form-decode-str}} options]
     (if (request :cookies)
       request
       (assoc request :cookies (parse-cookies request decoder))))))

(defn cookies-response
  "For responses with :cookies, adds Set-Cookie header and returns response
  without :cookies. See: wrap-cookies."
  {:added "1.2"}
  ([response]
   (cookies-response response {}))
  ([response options]
   (let [{:keys [encoder] :or {encoder codec/form-encode}} options]
     (-> response
         (set-cookies encoder)
         (dissoc :cookies)))))

(defn wrap-cookies
  "Parses the cookies in the request map, then assocs the resulting map
  to the :cookies key on the request.

  Accepts the following options:

  :decoder - a function to decode the cookie value. Expects a function that
             takes a string and returns a string. Defaults to URL-decoding.

  :encoder - a function to encode the cookie name and value. Expects a
             function that takes a name/value map and returns a string.
             Defaults to URL-encoding.

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
               (ie. prevent JavaScript access)
  :same-site - set to :strict or :lax to set SameSite attribute of the cookie"
  ([handler]
   (wrap-cookies handler {}))
  ([handler options]
   (fn
     ([request]
      (-> request
          (cookies-request options)
          handler
          (cookies-response options)))
     ([request respond raise]
      (handler (cookies-request request options)
               (fn [response] (respond (cookies-response response options)))
               raise)))))

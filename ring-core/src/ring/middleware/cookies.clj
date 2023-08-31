(ns ring.middleware.cookies
  "Middleware for parsing and generating cookies."
  (:import [java.time Duration ZonedDateTime ZoneId]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]
           [java.util Locale])
  (:require [ring.util.codec :as codec]
            [clojure.string :as str]
            [ring.util.parsing :refer [re-token]]))

;; RFC6265 regular expressions
(def ^:private re-cookie-octet
  #"[!#$%&'()*+\-./0-9:<=>?@A-Z\[\]\^_`a-z\{\|\}~]")

(def ^:private re-cookie-value
  (re-pattern (str "\"" re-cookie-octet "*\"|" re-cookie-octet "*")))

(def ^:private re-cookie
  (re-pattern (str "\\s*(" re-token ")=(" re-cookie-value ")\\s*[;,]?")))

(def ^:private set-cookie-attrs
  {:domain "Domain", :max-age "Max-Age", :path "Path"
   :secure "Secure", :expires "Expires", :http-only "HttpOnly"
   :same-site "SameSite"})

(def ^:private same-site-values
  {:strict "Strict", :lax "Lax", :none "None"})

(defn- parse-cookie-header [header]
  (for [[_ name value] (re-seq re-cookie header)]
    [name value]))

(defn- strip-quotes [value]
  (str/replace value #"^\"|\"$" ""))

(defn- decode-values [cookies decoder]
  (for [[name value] cookies]
    (when-let [value (decoder (strip-quotes value))]
      [name {:value value}])))

(defn- parse-cookies [request encoder]
  (if-let [cookie (get-in request [:headers "cookie"])]
    (->> cookie
         parse-cookie-header
         ((fn [c] (decode-values c encoder)))
         (remove nil?)
         (into {}))
    {}))

(defn- write-value [key value encoder]
  (encoder {key value}))

(defprotocol CookieInterval
  (->seconds [this]))

(defprotocol CookieDateTime
  (rfc822-format [this]))

(defn- class-by-name ^Class [s]
  (try (Class/forName s)
       (catch ClassNotFoundException _)))

(when-let [dt (class-by-name "org.joda.time.DateTime")]
  (extend dt
    CookieDateTime
    {:rfc822-format
     (eval
      '(let [fmtr (.. (org.joda.time.format.DateTimeFormat/forPattern
                       "EEE, dd MMM yyyy HH:mm:ss Z")
                      (withZone org.joda.time.DateTimeZone/UTC)
                      (withLocale java.util.Locale/US))]
         (fn [interval]
           (.print fmtr ^org.joda.time.DateTime interval))))}))

(when-let [interval (class-by-name "org.joda.time.Interval")]
  (extend interval
    CookieInterval
    {:->seconds
     (eval '(fn [dt] (.getSeconds (org.joda.time.Seconds/secondsIn dt))))}))

(extend-protocol CookieInterval
  Duration
  (->seconds [this]
    (.get this ChronoUnit/SECONDS)))

(let [java-rfc822-formatter
      (.. (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy HH:mm:ss Z")
          (withZone (ZoneId/of "UTC"))
          (withLocale Locale/US))]
  (extend-protocol CookieDateTime
    ZonedDateTime
    (rfc822-format [this]
      (.format java-rfc822-formatter this))))

(defn- valid-attr? [[key value]]
  (and (contains? set-cookie-attrs key)
       (not (.contains (str value) ";"))
       (case key
         :max-age (or (satisfies? CookieInterval value) (integer? value))
         :expires (or (satisfies? CookieDateTime value) (string? value))
         :same-site (contains? same-site-values value)
         true)))

(defn- write-attr-map [attrs]
  {:pre [(every? valid-attr? attrs)]}
  (for [[key value] attrs]
    (let [attr (name (set-cookie-attrs key))]
      (cond
        (satisfies? CookieInterval value) (str ";" attr "=" (->seconds value))
        (satisfies? CookieDateTime value) (str ";" attr "=" (rfc822-format value))
        (true? value) (str ";" attr)
        (false? value) ""
        (= :same-site key) (str ";" attr "=" (same-site-values value))
        :else (str ";" attr "=" value)))))

(defn- write-cookies [cookies encoder]
  (for [[key value] cookies]
    (if (map? value)
      (apply str (write-value key (:value value) encoder)
                 (write-attr-map (dissoc value :value)))
      (write-value key value encoder))))

(defn- set-cookies [response encoder]
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

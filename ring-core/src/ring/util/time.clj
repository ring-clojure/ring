(ns ring.util.time
  "Functions for dealing with time and dates in HTTP requests."
  (:require [clojure.string :as str])
  (:import java.text.SimpleDateFormat
           java.text.ParseException
           java.util.TimeZone
           java.util.Locale))

(def http-date-formats
  {:rfc1123 "EEE, dd MMM yyyy HH:mm:ss zzz"
   :rfc1036 "EEEE, dd-MMM-yy HH:mm:ss zzz"
   :asctime "EEE MMM d HH:mm:ss yyyy"})

(defn- ^SimpleDateFormat formatter [format]
  (doto (SimpleDateFormat. ^String (http-date-formats format) Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "GMT"))))

(defn- attempt-parse [date format]
  (try
    (.parse (formatter format) date)
    (catch ParseException _ nil)
    ;; Also catch RuntimeExceptions, needed for Clojure 1.3.0
    ;; See: https://groups.google.com/forum/#!topic/clojure/I5l1YHVMgkI
    (catch RuntimeException _ nil)))

(defn- trim-quotes [s]
  (str/replace s #"^'|'$" ""))

(defn parse-date
  "Attempt to parse a HTTP date. Returns nil if unsuccessful."
  [http-date]
  (->> (keys http-date-formats)
       (map (partial attempt-parse (trim-quotes http-date)))
       (remove nil?)
       (first)))
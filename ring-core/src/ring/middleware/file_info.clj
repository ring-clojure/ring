(ns ring.middleware.file-info
  "Augment Ring File responses."
  (:require [ring.util.response :as res])
  (:use [ring.util.mime-type :only (ext-mime-type)])
  (:import java.io.File
           (java.util Date Locale TimeZone)
           java.text.SimpleDateFormat))

(defn- guess-mime-type
  "Returns a String corresponding to the guessed mime type for the given file,
  or application/octet-stream if a type cannot be guessed."
  [^File file mime-types]
  (or (ext-mime-type (.getPath file) mime-types)
      "application/octet-stream"))

(defn ^SimpleDateFormat make-http-format
  "Formats or parses dates into HTTP date format (RFC 822/1123)."
  []
  ;; SimpleDateFormat is not threadsafe, so return a new instance each time
  (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn- not-modified-since?
  "Has the file been modified since the last request from the client?"
  [{headers :headers :as req} last-modified]
  (if-let [modified-since (headers "if-modified-since")]
    (not (.before (.parse (make-http-format) modified-since)
                  last-modified))))

(defn file-info-response
  "Adds headers to response as described in wrap-file-info."
  [{:keys [body] :as response} req & [mime-types]]
  (if (instance? File body)
    (let [file-type   (guess-mime-type body mime-types)
          file-length (.length ^File body)
          lmodified   (Date. (.lastModified ^File body))
          response    (-> response
                          (res/content-type file-type)
                          (res/header
                           "Last-Modified"
                           (.format (make-http-format) lmodified)))]
      (if (not-modified-since? req lmodified)
        (-> response (res/status 304)
            (res/header "Content-Length" 0)
            (assoc :body ""))
        (-> response (res/header "Content-Length" file-length))))
    response))

(defn wrap-file-info
  "Wrap an app such that responses with a file a body will have corresponding
  Content-Type, Content-Length, and Last Modified headers added if they can be
  determined from the file.
  If the request specifies a If-Modified-Since header that matches the last
  modification date of the file, a 304 Not Modified response is returned.
  If two arguments are given, the second is taken to be a map of file extensions
  to content types that will supplement the default, built-in map."
  [app & [mime-types]]
  (fn [req]
    (-> req
        app
        (file-info-response req mime-types))))

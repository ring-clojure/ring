(ns ring.middleware.file-info
  "Augment Ring File responses."
  (:require [ring.util.response :as res]
            [ring.middleware.not-modified :as not-modified])
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

(defn wrap-file-info
  "Wraps an app such that responses with a file body will have appropriate
  Content-Type, Content-Length, and Last Modified headers added if they can be
  determined from the file. If the request specifies a If-Modified-Since header
  that matches the last modification date of the file, a 304 Not Modified
  response is returned. If two arguments are given, the second is taken to be a
  map of file extensions to content types that will supplement the default,
  built-in map."
  [handler & [mime-types]]
  (not-modified/wrap-not-modified
   (fn [request]
     (let [{:keys [headers body] :as response} (handler request)]
       (if (instance? File body)
         (let [file-type   (guess-mime-type body mime-types)
               file-length (.length ^File body)
               lmodified   (Date. (.lastModified ^File body))]
           (-> response
               (res/content-type file-type)
               (res/header "Last-Modified" (.format (make-http-format) lmodified))
               (res/header "Content-Length" file-length)))
         response)))))
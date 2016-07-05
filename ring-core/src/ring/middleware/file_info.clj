(ns ring.middleware.file-info
  "Middleware to add Last-Modified and Content-Type headers to file responses.

  This middleware is deprecated. Prefer the ring.middleware.content-type and
  ring.middleware.not-modified middleware instead."
  (:require [ring.util.response :as res]
            [ring.util.mime-type :refer [ext-mime-type]]
            [ring.util.io :refer [last-modified-date]])
  (:import [java.io File]
           [java.util Date Locale TimeZone]
           [java.text SimpleDateFormat]))

(defn- guess-mime-type
  "Returns a String corresponding to the guessed mime type for the given file,
  or application/octet-stream if a type cannot be guessed."
  [^File file mime-types]
  (or (ext-mime-type (.getPath file) mime-types)
      "application/octet-stream"))

(defn- ^SimpleDateFormat make-http-format
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
  {:added "1.2", :deprecated "1.2"}
  ([response request]
   (file-info-response response request {}))
  ([response request mime-types]
   (let [body (:body response)]
     (if (instance? File body)
       (let [file-type   (guess-mime-type body mime-types)
             file-length (.length ^File body)
             lmodified   (last-modified-date body)
             response    (-> response
                             (res/content-type file-type)
                             (res/header
                              "Last-Modified"
                              (.format (make-http-format) lmodified)))]
         (if (not-modified-since? request lmodified)
           (-> response
               (res/status 304)
               (res/header "Content-Length" 0)
               (assoc :body ""))
           (-> response (res/header "Content-Length" file-length))))
       response))))

(defn wrap-file-info
  "Wrap a handler such that responses with a file for a body will have
  corresponding Content-Type, Content-Length, and Last Modified headers added if
  they can be determined from the file.

  If the request specifies a If-Modified-Since header that matches the last
  modification date of the file, a 304 Not Modified response is returned.
  If two arguments are given, the second is taken to be a map of file extensions
  to content types that will supplement the default, built-in map."
  {:deprecated "1.2"}
  ([handler]
   (wrap-file-info handler {}))
  ([handler mime-types]
   (fn
     ([request]
      (-> (handler request)
          (file-info-response request mime-types)))
     ([request respond raise]
      (handler request
               (fn [response]
                 (respond (file-info-response response request mime-types)))
               raise)))))

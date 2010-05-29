(ns ring.middleware.file-info
  "Augment Ring File responses."
  (:use [clojure.contrib.def :only (defvar-)])
  (:import java.io.File)
  (:import java.text.SimpleDateFormat)
  (:import java.util.SimpleTimeZone))

(defvar- base-mime-types
  {"ai"    "application/postscript"
   "asc"   "text/plain"
   "avi"   "video/x-msvideo"
   "bin"   "application/octet-stream"
   "bmp"   "image/bmp"
   "class" "application/octet-stream"
   "cer"   "application/pkix-cert"
   "crl"   "application/pkix-crl"
   "crt"   "application/x-x509-ca-cert"
   "css"   "text/css"
   "dms"   "application/octet-stream"
   "doc"   "application/msword"
   "dvi"   "application/x-dvi"
   "eps"   "application/postscript"
   "etx"   "text/x-setext"
   "exe"   "application/octet-stream"
   "gif"   "image/gif"
   "htm"   "text/html"
   "html"  "text/html"
   "jpe"   "image/jpeg"
   "jpeg"  "image/jpeg"
   "jpg"   "image/jpeg"
   "js"    "text/javascript"
   "lha"   "application/octet-stream"
   "lzh"   "application/octet-stream"
   "mov"   "video/quicktime"
   "mpe"   "video/mpeg"
   "mpeg"  "video/mpeg"
   "mpg"   "video/mpeg"
   "pbm"   "image/x-portable-bitmap"
   "pdf"   "application/pdf"
   "pgm"   "image/x-portable-graymap"
   "png"   "image/png"
   "pnm"   "image/x-portable-anymap"
   "ppm"   "image/x-portable-pixmap"
   "ppt"   "application/vnd.ms-powerpoint"
   "ps"    "application/postscript"
   "qt"    "video/quicktime"
   "ras"   "image/x-cmu-raster"
   "rb"    "text/plain"
   "rd"    "text/plain"
   "rtf"   "application/rtf"
   "sgm"   "text/sgml"
   "sgml"  "text/sgml"
   "swf"   "application/x-shockwave-flash"
   "tif"   "image/tiff"
   "tiff"  "image/tiff"
   "txt"   "text/plain"
   "xbm"   "image/x-xbitmap"
   "xls"   "application/vnd.ms-excel"
   "xml"   "text/xml"
   "xpm"   "image/x-xpixmap"
   "xwd"   "image/x-xwindowdump"
   "zip"   "application/zip"})

(defn- get-extension
  "Returns the file extension of a file."
  [#^File file]
  (second (re-find #"\.([^./\\]+)$" (.getPath file))))

(defn- guess-mime-type
  "Returns a String corresponding to the guessed mime type for the given file,
  or application/octet-stream if a type cannot be guessed."
  [#^File file mime-types]
  (get mime-types (get-extension file) "application/octet-stream"))

(defvar- http-date-formatter
  ;"A SimpleDateFormat instance, set to format using RFC 822/1123"
  (let [formatter (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ")]
    (do 
      ; We use GMT because it makes testing much easier
      (.setTimeZone formatter (SimpleTimeZone. 0 "GMT"))
      formatter)))

(defn- http-date
  "Takes a Date or Long, returns a String in HTTP Date (RFC 822/1123) format"
  [date]
  (.format http-date-formatter date))

(defn wrap-file-info
  "Wrap an app such that responses with a file a body will have 
  corresponding Content-Type, Content-Length, and Last Modified headers added 
  if they can be determined from the file.
  If two arguments are given, the second is taken to be a map of file extensions
  to content types that will supplement the default, built-in map.
  If the request specifies If-Modified-Since in its header, and it is a literal 
  match of the string returned as Last-Modified, a 304 with no body will be 
  sent instead."
  [app & [custom-mime-types]]
  (let [mime-types (merge base-mime-types custom-mime-types)]
    (fn [req]
      (let [{:keys [headers body] :as response} (app req)]
        (if (instance? File body)
          (let [
                 file-size        (str (.length #^File body))
                 content-type     (guess-mime-type body mime-types)
                 server-lmodified (http-date (.lastModified body))
                 client-lmodified (get (:headers req) "if-modified-since")
                 ;it'd be nice to have a real date comparison at some point
                 not-modified     (= client-lmodified server-lmodified)]
            (if not-modified
              (assoc response :status 304 :body "" :headers 
                (assoc headers "Content-Length" "0"
                               "Content-Type"   content-type
                               "Last-Modified"  server-lmodified))
              (assoc response :headers
                (assoc headers "Content-Length" file-size
                               "Content-Type"   content-type
                               "Last-Modified"  server-lmodified))))
          response)))))

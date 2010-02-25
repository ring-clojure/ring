(ns ring.util.request)

(defn ssl?
  "Returns true iff the request was submitted over SSL."
  [req]
  (= :https (:scheme req)))

(defn server-host
  "Returns a String for the full hostname."
  [req]
  (let [hdrs (:headers req)]
    (or (get hdrs "x-forwarded-host")
        (get hdrs "host")
        (:server-name req))))

(defn full-uri
  "Returns a String for the full request URI, including the protocol and host."
  [req]
  (str (name (:scheme req)) "://" (server-host req) (:uri req)))

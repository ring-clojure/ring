(ns ring.middleware.authorization
  (:refer-clojure :exclude [update-keys])
  (:require
    [clojure.string :as str]
    [ring.util.codec :as codec]
    [ring.util.request :as request]))

;; Copied from Clojure 1.11:
(defn- update-keys
  [m f]
  (let [ret (persistent!
              (reduce-kv (fn [acc k v] (assoc! acc (f k) v))
                         (transient {})
                         m))]
    (with-meta ret (meta m))))

(defn authorization-request
  "Parses `Authorization` header in the request map. See: wrap-authorization.

  See RFC 7235 Section 2 (https://datatracker.ietf.org/doc/html/rfc7235#section-2),
  and RFC 9110 Section 11 (https://datatracker.ietf.org/doc/html/rfc9110#section-11)."
  {:added "1.12"}
  [request]
  (if (:authorization request)
    request
    (assoc request :authorization (some-> (request/authorization request)
                                          (update :scheme str/lower-case)
                                          (update :params update-keys str/lower-case)))))

(defn wrap-authorization
  "Parses the `Authorization` header in the request map, then assocs the result
  to the :authorization key on the request.

  See RFC 7235 Section 2 (https://datatracker.ietf.org/doc/html/rfc7235#section-2),
  and RFC 9110 Section 11 (https://datatracker.ietf.org/doc/html/rfc9110#section-11)."
  {:added "1.12"}
  [handler]
  (fn
    ([request]
     (handler (authorization-request request)))
    ([request respond raise]
     (handler (authorization-request request)
              respond
              raise))))

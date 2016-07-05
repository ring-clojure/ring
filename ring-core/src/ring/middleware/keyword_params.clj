(ns ring.middleware.keyword-params
  "Middleware that converts parameter keys in the request to keywords.")

(defn- keyword-syntax? [s]
  (re-matches #"[A-Za-z*+!_?-][A-Za-z0-9*+!_?-]*" s))

(defn- keyify-params [target]
  (cond
    (map? target)
      (into {}
        (for [[k v] target]
          [(if (and (string? k) (keyword-syntax? k))
             (keyword k)
             k)
           (keyify-params v)]))
    (vector? target)
      (vec (map keyify-params target))
    :else
      target))

(defn keyword-params-request
  "Converts string keys in :params map to keywords. See: wrap-keyword-params."
  {:added "1.2"}
  [request]
  (update-in request [:params] keyify-params))

(defn wrap-keyword-params
  "Middleware that converts the any string keys in the :params map to keywords.
  Only keys that can be turned into valid keywords are converted.

  This middleware does not alter the maps under :*-params keys. These are left
  as strings."
  [handler]
  (fn
    ([request]
     (handler (keyword-params-request request)))
    ([request respond raise]
     (handler (keyword-params-request request) respond raise))))

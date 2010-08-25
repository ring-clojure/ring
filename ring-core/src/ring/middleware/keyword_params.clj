(ns ring.middleware.keyword-params
  "Convert param keys to keywords.")

(defn- keyify-params [target]
  (cond
    (map? target)
      (reduce
        (fn [m [k v]]
          (assoc m (keyword k) (keyify-params v)))
        {}
        target)
    (vector? target)
      (vec (map keyify-params target))
    :else
      target))

(defn wrap-keyword-params
  "Middleware that converts the string-keyed :params map to one with keyword
  keys before forwarding the request to the given handler.
  Does not alter the maps under :*-params keys; these are left with strings."
  [handler]
  (fn [req]
    (handler (update-in req [:params] keyify-params))))

(ns ring.middleware.keyword-params
  "Convert param keys to keywords.")

(defn- keyword-syntax? [s]
  (re-matches #"(?:[A-Za-z*+!_?-][\w*+!_?-]*\.)*?[A-Za-z*+!_?-][\w*+!_?-]*(?:/(?:[A-Za-z*+!_?-][\w*+!_?-]*)*)?" s))

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
  "Converts string keys in :params map to keywords."
  [req]
  (update-in req [:params] keyify-params))

(defn wrap-keyword-params
  "Middleware that converts the string-keyed :params map to one with keyword
  keys before forwarding the request to the given handler.
  Does not alter the maps under :*-params keys; these are left with strings."
  [handler]
  (fn [req]
    (-> req
        keyword-params-request
        handler)))

(ns ring.middleware.keyword-params
  "Convert param keys to keywords.")

(def ^:private keyword-re #"[A-Za-z*+!_?-][A-Za-z0-9*+!_?-]*")
(def ^:private namespaced-keyword-re
  (re-pattern (str "([A-Za-z*+!_?-][A-Za-z0-9*+!_?.-]*/)?" keyword-re)))

(defn- keyword-syntax? [s allow-namespaces?]
  (re-matches
   (if allow-namespaces? namespaced-keyword-re keyword-re)
   s))

(defn- keyify-params [target allow-namespaces?]
  (cond
    (map? target)
      (into {}
        (for [[k v] target]
          [(if (and (string? k) (keyword-syntax? k allow-namespaces?))
             (keyword k)
             k)
           (keyify-params v allow-namespaces?)]))
    (vector? target)
    (vec (map #(keyify-params % allow-namespaces?) target))
    :else
      target))

(defn keyword-params-request
  "Converts string keys in :params map to keywords."
  [req & [{:keys [allow-namespaces?]
           :or {:allow-namespaces? false}}]]
  (update-in req [:params] keyify-params allow-namespaces?))

(defn wrap-keyword-params
  "Middleware that converts the string-keyed :params map to one with keyword
  keys before forwarding the request to the given handler.
  Does not alter the maps under :*-params keys; these are left with strings.
  Takes an optional configuration map. Recognized keys are:
    :allow-namespaces? - defaults to false"
  [handler & [opts]]
  (fn [req]
    (-> req
        (keyword-params-request opts)
        handler)))

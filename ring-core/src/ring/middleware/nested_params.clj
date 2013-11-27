(ns ring.middleware.nested-params
  "Convert a single-depth map of parameters to a nested map.")

(def nested-param-pattern #"\]$|\.")

(defn parse-nested-keys
  "Parse a parameter name into a list of keys using a 'C'-like index
  notation. e.g.
    \"foo[bar][][baz]\"
    => [\"foo\" \"bar\" \"\" \"baz\"]"
  [param-name]
  (let [[_ k ks] (re-matches #"(.*?)((?:\[.*?\])*)" (name param-name))
        keys     (if ks (map second (re-seq #"\[(.*?)\]" ks)))]
    (cons k keys)))

(defn- assoc-nested
  "Similar to assoc-in, but treats values of blank keys as elements in a
  list."
  [m [k & ks] v]
  (conj m
        (if k
          (if-let [[j & js] ks]
            (if (= j "")
              {k (assoc-nested (get m k []) js v)}
              {k (assoc-nested (get m k {}) ks v)})
            {k v})
          v)))

(defn- param-pairs
  "Return a list of name-value pairs for a parameter map."
  [params]
  (mapcat
    (fn [[name value]]
      (if (sequential? value)
        (for [v value] [name v])
        [[name value]]))
    params))

(defn- nest-params
  "Takes a flat map of parameters and turns it into a nested map of
  parameters, using the function parse to split the parameter names
  into keys."
  [params parse]
  (reduce
   (fn [m [k v]]
     (if (re-find nested-param-pattern k)
       (assoc-nested m (parse k) v)
       (update-in m [k] #(if (nil? %) v (conj [%] v)))))
   {}
   (param-pairs params)))

(defn nested-params-request
  "Converts a request with a flat map of parameters to a nested map."
  [request & [opts]]
  (let [parse (:key-parser opts parse-nested-keys)]
    (update-in request [:params] nest-params parse)))

(defn wrap-nested-params
  "Middleware to converts a flat map of parameters into a nested map.

  Uses the function in the :key-parser option to convert parameter names
  to a list of keys. Values in keys that are empty strings are treated
  as elements in a list. Defaults to using the parse-nested-keys function.

  e.g.
    {\"foo[bar]\" \"baz\"}
    => {\"foo\" {\"bar\" \"baz\"}}

    {\"foo[]\" \"bar\"}
    => {\"foo\" [\"bar\"]}"
  [handler & [opts]]
  (fn [request]
    (-> request
        (nested-params-request opts)
        handler)))

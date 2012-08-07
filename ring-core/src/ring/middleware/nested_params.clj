(ns ring.middleware.nested-params
  "Convert a single-depth map of parameters to a nested map.")

(defn parse-nested-keys
  "Parse a parameter name into a list of keys using a 'C'-like index
  notation. e.g.
    \"foo[bar][][baz]\"
    => [\"foo\" \"bar\" \"\" \"baz\"]"
  [param-name]
  (if (= (first param-name) \[)
    (read-string param-name)
    (let [[_ k ks] (re-matches #"(.*?)((?:\[.*?\])*)" (name param-name))
          keys     (if ks (map second (re-seq #"\[(.*?)\]" ks)))]
      (cons k keys))))

(defn- assoc-nested
  "Similar to assoc-in, but treats values of blank keys as elements in a
  list."
  [m [k & ks] v]
  (conj m
        (if k
          (if-let [[j & js] ks]
            (cond
             (number? j) (let [nested-vec (get m k [])
                               nested-cnt (count nested-vec)
                               nested-vec (if (< j nested-cnt)
                                            nested-vec
                                            (into nested-vec (repeat (inc (- j nested-cnt)) nil)))
                               nested-vec (update-in nested-vec [j] (fnil assoc-nested {}) js v)]
                           {k nested-vec})
             (= j "") {k (assoc-nested (get m k []) js v)}
             :else {k (assoc-nested (get m k {}) ks v)})
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
      (assoc-nested m (parse k) v))
    {}
    (param-pairs params)))

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
    (let [parse   (:key-parser opts parse-nested-keys)
          request (update-in request [:params] nest-params parse)]
      (handler request))))

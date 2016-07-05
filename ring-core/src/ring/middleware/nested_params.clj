(ns ring.middleware.nested-params
  "Middleware to convert a single-depth map of parameters to a nested map."
  (:require [ring.util.codec :refer [assoc-conj]]))

(defn parse-nested-keys
  "Parse a parameter name into a list of keys using a 'C'-like index
  notation.

  For example:

    \"foo[bar][][baz]\"
    => [\"foo\" \"bar\" \"\" \"baz\"]"
  [param-name]
  (let [[_ k ks] (re-matches #"(?s)(.*?)((?:\[.*?\])*)" (name param-name))
        keys     (if ks (map second (re-seq #"\[(.*?)\]" ks)))]
    (cons k keys)))

(defn- assoc-vec [m k v]
  (let [m (if (contains? m k) m (assoc m k []))]
    (assoc-conj m k v)))

(defn- assoc-nested
  "Similar to assoc-in, but treats values of blank keys as elements in a
  list."
  [m [k & ks] v]
  (if k
    (if ks
      (let [[j & js] ks]
        (if (= j "")
          (assoc-vec m k (assoc-nested {} js v))
          (assoc m k (assoc-nested (get m k {}) ks v))))
      (if (map? m)
        (assoc-conj m k v)
        {k v}))
    v))

(defn- param-pairs
  "Return a list of name-value pairs for a parameter map."
  [params]
  (mapcat
    (fn [[name value]]
      (if (and (sequential? value) (not (coll? (first value))))
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

(defn nested-params-request
  "Converts a request with a flat map of parameters to a nested map.
  See: wrap-nested-params."
  {:added "1.2"}
  ([request]
   (nested-params-request request {}))
  ([request options]
   (let [parse (:key-parser options parse-nested-keys)]
     (update-in request [:params] nest-params parse))))

(defn wrap-nested-params
  "Middleware to converts a flat map of parameters into a nested map.
  Accepts the following options:

  :key-parser - the function to use to parse the parameter names into a list
                of keys. Keys that are empty strings are treated as elements in
                a vector, non-empty keys are treated as elements in a map.
                Defaults to the parse-nested-keys function.

  For example:

    {\"foo[bar]\" \"baz\"}
    => {\"foo\" {\"bar\" \"baz\"}}

    {\"foo[]\" \"bar\"}
    => {\"foo\" [\"bar\"]}"
  ([handler]
   (wrap-nested-params handler {}))
  ([handler options]
   (fn
     ([request]
      (handler (nested-params-request request options)))
     ([request respond raise]
      (handler (nested-params-request request options) respond raise)))))

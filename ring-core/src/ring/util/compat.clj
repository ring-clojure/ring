(ns ring.util.compat
  "Functions and macros for managing compatibility between different versions
  of Clojure.")

(defmacro compile-if
  "If an expression can be compiled, return 'then', otherwise 'else'."
  ([exp then] `(compile-if ~exp ~then nil))
  ([exp then else]
     (if (try (eval exp) (catch Throwable _ false))
       `(do ~then)
       `(do ~else))))

(defn reducible?
  "Return true iff x is a reduce-ible value."
  [x]
  (compile-if clojure.core.protocols/CollReduce
    (satisfies? clojure.core.protocols/CollReduce x)
    (seq? x)))

(ns ring.util.cond-compile)

(defmacro cond-compile
  "Similar to cond, but at macro level.
  The purpose of this macro is to test for language features at compile time.
  Conditions are evaluated at macro expansion time and the expression
  of the first true condition is the result to be compiled."
  [& clauses]
  {:pre [(even? (count clauses))]}
  (let [clause-thunks (partition 2 clauses)
        [[_ thunk]] (filter #(eval (first %)) clause-thunks)]
    thunk))

(def ^:private clojure-core (find-ns 'clojure.core))

(defn resolve-clojure
  "Try to resolve a var from clojure.core"
  [s]
  (ns-resolve clojure-core s))

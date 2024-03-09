(ns ring.util.async)

(defn raising
  "Alter function f to catch all Throwable errors and pass them to the raise
  function. Designed for use with asynchronous handlers, particularly in
  wrapping a respond argument to a 3-arity handler."
  {:added "1.12"}
  [raise f]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable t (raise t)))))

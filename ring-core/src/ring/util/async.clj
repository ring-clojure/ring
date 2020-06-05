(ns ring.util.async)

(defn raising
  "Alter function f to catch all Throwable errors and pass them to the raise
  function. Designed for use with asynchronous handlers."
  [raise f]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable t (raise t)))))

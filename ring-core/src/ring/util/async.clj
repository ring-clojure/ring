(ns ring.util.async)

(defmacro wrap-ring-async
  "Wraps a response altering expression in middleware such that it is
  also applied to the response function given to an async reactor when
  the type is :ring."
  [[sym resp-expr] alter-expr]
  `(when-let [~sym ~resp-expr]
     (if-let [type# (:async ~sym)]
       (if (= type# :ring)
         (let [reactor# (:reactor ~sym)]
           (assoc ~sym :reactor
                  (fn [response-fn#]
                    (reactor#
                     (fn [~sym]
                       (response-fn# ~alter-expr))))))
         ~sym)
       ~alter-expr)))


(defmacro with-ring-async
  "A small macro that binds a ring response function to the given
  symbol, which can be used in the given body."
  [[sym] & body]
  `(let [reactor# (fn [~sym] ~@body)]
     {:async :ring
      :reactor reactor#}))

(ns ring.builder)

(defn wrap-if
  "If test is logically true, returns the result of invoking the wrapper on the 
  core app, i.e. a wrapped app; if test is logically false, returns the core
  app."
  [test wrapper-fn core-app]
  (if test (wrapper-fn core-app) core-app))
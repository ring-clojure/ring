(ns ring.reloading)

(defn wrap
  "Wrap an app such that before a request is passed to the app, each namespace
  identified by syms in reloadables is reloaded."
  [reloadables app]
  (fn [req]
    (doseq [ns-sym reloadables]
      (require ns-sym :reload))
    (app req)))
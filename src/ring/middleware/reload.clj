(ns ring.middleware.reload)

(defn wrap-reload
  "Wrap an app such that before a request is passed to the app, each namespace
  identified by syms in reloadables is reloaded.
  Currently this requires that the namespaces in question are being (re)loaded
  from un-jarred source files, as apposed to source files in jars or compiled
  classes."
  [app reloadables]
  (fn [req]
    (doseq [ns-sym reloadables]
      (require ns-sym :reload))
    (app req)))
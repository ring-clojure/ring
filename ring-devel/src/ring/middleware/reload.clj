(ns ring.middleware.reload
  "Middleware that reloads modified namespaces on each request.

  This middleware should be limited to use in development environments."
  (:require [ns-tracker.core :refer [ns-tracker]]))

(defn wrap-reload
  "Reload namespaces of modified files before the request is passed to the
  supplied handler.

  Accepts the following options:

  :dirs - A list of directories that contain the source files.
          Defaults to [\"src\"]."
  {:arglists '([handler] [handler options])}
  [handler & [options]]
  (let [source-dirs (:dirs options ["src"])
        modified-namespaces (ns-tracker source-dirs)
        uncompiled (atom [])]
    (fn [request]
      (let [uncompiled-ns @uncompiled]
        (reset! uncompiled [])
        (doseq [ns-sym (concat uncompiled-ns (modified-namespaces))]
          (try
            (require ns-sym :reload)
            (catch Exception ex
              (swap! uncompiled conj ns-sym)
              (throw ex)))))
      (handler request))))

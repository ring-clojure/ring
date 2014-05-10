(ns ring.middleware.reload
  "Middleware that reloads modified namespaces on each request.

  This middleware should be limited to use in development environments."
  (:use [ns-tracker.core :only (ns-tracker)]))

(defn wrap-reload
  "Reload namespaces of modified files before the request is passed to the
  supplied handler.

  Accepts the following options:

  :dirs - A list of directories that contain the source files.
          Defaults to [\"src\"]."
  [handler & [options]]
  (let [source-dirs (:dirs options ["src"])
        modified-namespaces (ns-tracker source-dirs)]
    (fn [request]
      (doseq [ns-sym (modified-namespaces)]
        (require ns-sym :reload))
      (handler request))))

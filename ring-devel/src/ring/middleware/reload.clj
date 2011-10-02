(ns ring.middleware.reload
  "Reload modified namespaces on the fly."
  (:use [ns-tracker.core :only (ns-tracker)]))

(defn wrap-reload
  "Reload namespaces of modified files before the request is passed to the
  supplied handler.

  Takes the following options:
    :dirs - A list of directories that contain the source files.
            Defaults to [\"src\"]."
  [handler & [options]]
  (let [source-dirs (:dirs options ["src"])
        modified-namespaces (ns-tracker source-dirs)]
    (fn [request]
      (doseq [ns-sym (modified-namespaces)]
        (require ns-sym :reload))
      (handler request))))

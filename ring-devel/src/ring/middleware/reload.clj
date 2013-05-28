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

(ns ring.middleware.reload
  "Middleware that reloads modified namespaces on each request.

  This middleware should be limited to use in development environments."
  (:require [ns-tracker.core :refer [ns-tracker]]))

(defn- reloader [dirs retry?]
  (let [modified-namespaces (ns-tracker dirs)
        load-queue (java.util.concurrent.LinkedBlockingQueue.)]
    (fn []
      (locking load-queue
        (doseq [ns-sym (modified-namespaces)]
          (.offer load-queue ns-sym))
        (loop []
          (when-let [ns-sym (.peek load-queue)]
            (if retry?
              (do (require ns-sym :reload) (.remove load-queue))
              (do (.remove load-queue) (require ns-sym :reload)))
            (recur)))))))

(defn wrap-reload
  "Reload namespaces of modified files before the request is passed to the
  supplied handler.

  Accepts the following options:

  :dirs                   - A list of directories that contain the source files.
                            Defaults to [\"src\"].
  :reload-compile-errors? - If true, keep attempting to reload namespaces
                            that have compile errors.  Defaults to true."
  ([handler]
   (wrap-reload handler {}))
  ([handler options]
   (let [reload! (reloader (:dirs options ["src"])
                           (:reload-compile-errors? options true))]
     (fn
       ([request]
        (reload!)
        (handler request))
       ([request respond raise]
        (reload!)
        (handler request respond raise))))))

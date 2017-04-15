(ns ring.middleware.context
  "Middleware for making a handler behave like it is a context handler."
  (:require [clojure.string :refer [split]]))

(defn wrap-context
  "Middleware that makes a handler behave like a context handler. This means
  that it splits the :uri putting the first segment in :context and the rest
  in :path-info. The context without the leading / is passed to match-context?
  to determine if the handler should be called at all. In this way, multiple
  context handlers can be combined."
  ([handler]
     (wrap-context handler (constantly true)))
  ([handler match-context?]
     (fn [request]
       (let [[_ context path-info] (split (:uri request) #"/" 3)]
         (when (match-context? context)
           (handler (assoc request
                      :context (str "/" context)
                      :path-info (str "/" path-info))))))))

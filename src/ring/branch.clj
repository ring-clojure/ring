(ns ring.branch)

(defn wrap
  "Returns an app that proxies to one of several possible backing apps depending
  on the beginning of the request uri.
  Should be passed an odd number of arguments - the inital pairs of arguments
  indicate the branching conditions and corresponding apps, and the last
  argument the fallback app that will be invoked if none of the branching
  conditions are met:
  
  (wrap [\"/javascripts\" \"stylesheets\"] static-app
        [\"/blog\"] blog-app
        dynamic-app)"
 [& branching]
 (let [fallback     (last branching)
       branch-logic
         (map (fn [[prefixes branch]]
                (fn [req]
                  (let [uri (:uri req)]
                    (if (some #(.startsWith uri %) prefixes) (branch req)))))
              (partition 2 branching))]
    (fn [req]
      (or (some #(% req) branch-logic)
          (fallback req)))))
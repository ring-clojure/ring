(ns ring.util.compat
  "Functions for cross-compatibility between Ring 1 and Ring 2.")

(defn modify-req
  "Apply a function f to both a request that may have Ring 1 or Ring 2 keys in
  the form: (f req k & vs). The supplied key, k, will be namespaced for Ring 2,
  and stripped of it's namespace for Ring 1."
  ([req f k]
   (-> req
       (cond-> (contains? req :ring.request/method)
         (f k))
       (cond-> (contains? req :request-method)
         (f (keyword (name k))))))
  ([req f k v1]
   (modify-req req #(f %1 %2 v1) k))
  ([req f k v1 v2]
   (modify-req req #(f %1 %2 v1 v2) k))
  ([req f k v1 v2 & vs]
   (modify-req req #(apply f %1 %2 v1 v2 vs) k)))

(defn get-req
  "Get the value of a namespaced keyword on the request map. If it fails, try
  the unnamespaced version."
  [req k]
  (get req k (get req (keyword (name k)))))

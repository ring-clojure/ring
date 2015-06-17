(ns ring.middleware.flash
  "Middleware that adds session-based flash store that persists only to the
  next request in the same session."
  (:require [ring.util.async :refer (wrap-ring-async)]))

(defn flash-request
  "Adds :flash key to request from :_flash in session."
  {:added "1.2"}
  [request]
  (let [session (:session request)
        flash   (:_flash session)
        session (dissoc session :_flash)]
    (assoc request :session session, :flash flash)))

(defn flash-response
  "If response has a :flash key, saves it in :_flash of session for next request."
  {:arglists '([response request])
   :added "1.2"}
  [response {:keys [session flash] :as request}]
  (let [session (if (contains? response :session)
                  (response :session)
                  session)
        session (if-let [flash (response :flash)]
                  (assoc (response :session session) :_flash flash)
                  session)]
    (if (or flash (response :flash) (contains? response :session))
      (assoc response :session session)
      response)))

(defn wrap-flash
  "If a :flash key is set on the response by the handler, a :flash key with
  the same value will be set on the next request that shares the same session.
  This is useful for small messages that persist across redirects."
  [handler]
  (fn [request]
    (wrap-ring-async [resp (handler (flash-request request))]
      (flash-response resp (flash-request request)))))

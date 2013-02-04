(ns ring.middleware.flash
  "A session-based flash store that persists to the next request.")

(defn flash-request
  "Adds :flash key to request from :_flash in session."
  [request]
  (let [session (:session request)
        flash   (:_flash session)
        session (dissoc session :_flash)]
    (assoc request :session session, :flash flash)))

(defn flash-response
  "If response has a :flash key, saves it in :_flash of session for next request."
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
    (when-let [resp (-> request
                        flash-request
                        handler)]
      (flash-response resp request))))

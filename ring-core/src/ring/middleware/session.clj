(ns ring.middleware.session
  "Session manipulation."
  (:require [ring.middleware.cookies :as cookies]
            [ring.middleware.session.store :as store]
            [ring.middleware.session.memory :as mem]))

(defn session-options
  [options]
  {:store (options :store (mem/memory-store))
   :cookie-name (options :cookie-name "ring-session")
   :cookie-attrs (merge {:path "/"}
                        (options :cookie-attrs)
                        (if-let [root (options :root)]
                          {:path root}))})

(defn- bare-session-request
  [request & [{:keys [store cookie-name]}]]
  (let [req-key  (get-in request [:cookies cookie-name :value])
        session  (store/read-session store req-key)
        session-key (if session req-key)]
    (merge request {:session (or session {})
                    :session/key session-key})))

(defn session-request
  "Reads current HTTP session map and adds it to :session key of the request."
  [request & [opts]]
  (-> request
      cookies/cookies-request
      (bare-session-request opts)))

(defn- bare-session-response
  [response {session-key :session/key}  & [{:keys [store cookie-name cookie-attrs]}]]
  (let [new-session-key (when (contains? response :session)
                          (if-let [session (response :session)]
                            (store/write-session store session-key session)
                            (when session-key
                              (store/delete-session store session-key))))
        response (dissoc response :session)
        cookie   {cookie-name
                  (merge cookie-attrs
                         (response :session-cookie-attrs)
                         {:value new-session-key})}]
    (if (and new-session-key (not= session-key new-session-key))
      (assoc response :cookies (merge (response :cookies) cookie))
      response)))

(defn session-response
  "Updates session based on :session key in response."
  [response request & [opts]]
  (when response
    (-> response
        (bare-session-response request opts)
        cookies/cookies-response)))

(defn wrap-session
  "Reads in the current HTTP session map, and adds it to the :session key on
  the request. If a :session key is added to the response by the handler, the
  session is updated with the new value. If the value is nil, the session is
  deleted.

  The following options are available:
    :store
      An implementation of the SessionStore protocol in the
      ring.middleware.session.store namespace. This determines how the
      session is stored. Defaults to in-memory storage
      (ring.middleware.session.store.MemoryStore).
    :root
      The root path of the session. Any path above this will not be able to
      see this session. Equivalent to setting the cookie's path attribute.
      Defaults to \"/\".
    :cookie-name
      The name of the cookie that holds the session key. Defaults to
      \"ring-session\"
    :cookie-attrs
      A map of attributes to associate with the session cookie. Defaults
      to {}."
  ([handler]
     (wrap-session handler {}))
  ([handler options]
     (let [options (session-options options)]
       (fn [request]
         (let [new-request (session-request request options)]
           (-> new-request
               handler
               (session-response new-request options)))))))

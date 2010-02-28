(ns ring.middleware.session
  "Session manipulation."
  (:use ring.middleware.cookies
        ring.middleware.session.memory))

(defn wrap-session
  "Reads in the current HTTP session map, and adds it to the :session key on
  the request. If a :session key is added to the response by the handler, the
  session is updated with the new value. If the value is nil, the session is
  deleted.

  The following options are available:
    :store
      An implementation map containing :read, :write, and :delete
      keys. This determines how the session is stored. Defaults to
      in-memory storage.
    :cookie-name
      The name of the cookie that holds the session key. Defaults to
      \"ring-session\""
  ([handler]
    (wrap-session handler {}))
  ([handler options]
    (let [store  (options :store (memory-store))
          cookie (options :cookie-name "ring-session")]
      (wrap-cookies
        (fn [request]
          (let [sess-key (get-in request [:cookies cookie :value])
                session  ((store :read) sess-key)
                request  (assoc request :session session)
                response (handler request)
                sess-key* (if (contains? response :session)
                            (if (response :session)
                              ((store :write) sess-key (response :session))
                              (if sess-key
                                ((store :delete) sess-key))))
                response (dissoc response :session)]
              (if (and sess-key* (not= sess-key sess-key*))
                (assoc response :cookies {cookie sess-key*})
                response)))))))

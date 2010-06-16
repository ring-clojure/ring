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
    :root
      The root path of the session. Anything path above this will not
      be able to see this session. Equivalent to setting the cookie's
      path attribute. Defaults to \"/\".
    :cookie-name
      The name of the cookie that holds the session key. Defaults to
      \"ring-session\"
    :cookie-attrs
      A map of attributes to associate with the session cookie. Defaults
      to {}."
  ([handler]
    (wrap-session handler {}))
  ([handler options]
     (let [store        (options :store (memory-store))
           cookie-name  (options :cookie-name "ring-session")
           session-root (options :root "/")
           cookie-attrs (merge (options :cookie-attrs) {:path session-root})]
      (wrap-cookies
        (fn [request]
          (let [sess-key (get-in request [:cookies cookie-name :value])
                session  ((store :read) sess-key)
                request  (assoc request :session session)
                response (handler request)
                sess-key* (if (contains? response :session)
                            (if (response :session)
                              ((store :write) sess-key (response :session))
                              (if sess-key
                                ((store :delete) sess-key))))
                response (dissoc response :session)
                cookie   {cookie-name (merge cookie-attrs
                                             (response :session-cookie-attrs)
                                             {:value sess-key*})}]
            (if (and sess-key* (not= sess-key sess-key*))
              (assoc response :cookies (merge (response :cookies) cookie))
              response)))))))

(ns ring.middleware.test.session
  (:use clojure.test
        ring.middleware.session
        ring.middleware.session.store))

(defn- make-store [reader writer deleter]
  (reify SessionStore
    (read-session [_ k] (reader k))
    (write-session [_ k s] (writer k s))
    (delete-session [_ k] (deleter k))))

(defn trace-fn [f]
  (let [trace (atom [])]
    (with-meta
      (fn [& args]
        (swap! trace conj args)
        (apply f args))
      {:trace trace})))

(defn trace [f]
  (-> f meta :trace deref))

(deftest session-is-read
  (let [reader   (trace-fn (constantly {:bar "foo"}))
        writer   (trace-fn (constantly nil))
        deleter  (trace-fn (constantly nil))
        store    (make-store reader writer deleter)
        handler  (trace-fn (constantly {}))
        handler* (wrap-session handler {:store store})]
    (handler* {:cookies {"ring-session" {:value "test"}}})
    (is (= (trace reader) [["test"]]))
    (is (= (trace writer) []))
    (is (= (trace deleter) []))
    (is (= (-> handler trace first first :session)
           {:bar "foo"}))))

(deftest session-is-written
  (let [reader  (trace-fn (constantly {}))
        writer  (trace-fn (constantly nil))
        deleter (trace-fn (constantly nil))
        store   (make-store reader writer deleter)
        handler (constantly {:session {:foo "bar"}})
        handler (wrap-session handler {:store store})]
    (handler {:cookies {}})
    (is (= (trace reader) [[nil]]))
    (is (= (trace writer) [[nil {:foo "bar"}]]))
    (is (= (trace deleter) []))))

(deftest session-is-deleted
  (let [reader  (trace-fn (constantly {}))
        writer  (trace-fn (constantly nil))
        deleter (trace-fn (constantly nil))
        store   (make-store reader writer deleter)
        handler (constantly {:session nil})
        handler (wrap-session handler {:store store})]
    (handler {:cookies {"ring-session" {:value "test"}}})
    (is (= (trace reader) [["test"]]))
    (is (= (trace writer) []))
    (is (= (trace deleter) [["test"]]))))

(deftest session-write-outputs-cookie
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
        handler (constantly {:session {:foo "bar"}})
        handler (wrap-session handler {:store store})
        response (handler {:cookies {}})]
    (is (= (get-in response [:headers "Set-Cookie"])
           ["ring-session=foo%3Abar;Path=/"]))))

(deftest session-delete-outputs-cookie
  (let [store (make-store (constantly {:foo "bar"})
                          (constantly nil)
                          (constantly "deleted"))
        handler (constantly {:session nil})
        handler (wrap-session handler {:store store})
        response (handler {:cookies {"ring-session" {:value "foo:bar"}}})]
    (is (= (get-in response [:headers "Set-Cookie"])
           ["ring-session=deleted;Path=/"]))))

(deftest session-cookie-has-attributes
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}})
	handler (wrap-session handler {:store store
                                       :cookie-attrs {:max-age 5 :path "/foo"}})
	response (handler {:cookies {}})]
    (is (= (get-in response [:headers "Set-Cookie"])
	   ["ring-session=foo%3Abar;Max-Age=5;Path=/foo"]))))

(deftest session-does-not-clobber-response-cookies
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}
			     :cookies {"cookie2" "value2"}})
	handler (wrap-session handler {:store store :cookie-attrs {:max-age 5}})
	response (handler {:cookies {}})]
    (is (= (get-in response [:headers "Set-Cookie"])
	   ["ring-session=foo%3Abar;Max-Age=5;Path=/" "cookie2=value2"]))))

(deftest session-root-can-be-set
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}})
	handler (wrap-session handler {:store store, :root "/foo"})
	response (handler {:cookies {}})]
    (is (= (get-in response [:headers "Set-Cookie"])
	   ["ring-session=foo%3Abar;Path=/foo"]))))

(deftest session-attrs-can-be-set-per-request
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}
                             :session-cookie-attrs {:max-age 5}})
	handler (wrap-session handler {:store store})
	response (handler {:cookies {}})]
    (is (= (get-in response [:headers "Set-Cookie"])
	   ["ring-session=foo%3Abar;Max-Age=5;Path=/"]))))

(deftest session-response-is-nil
  (let [handler (wrap-session (constantly nil))]
    (is (nil? (handler {})))))

(deftest session-made-up-key
  (let [store-ref (atom {})
        store     (make-store
                   #(@store-ref %)
                   #(do (swap! store-ref assoc %1 %2) %1)
                   #(do (swap! store-ref dissoc %) nil))
        handler   (wrap-session
                   (constantly {:session {:foo "bar"}})
                   {:store store})]
    (handler {:cookies {"ring-session" {:value "faked-key"}}})
    (is (not (contains? @store-ref "faked-key")))))

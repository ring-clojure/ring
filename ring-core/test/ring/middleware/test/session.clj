(ns ring.middleware.test.session
  (:require [clojure.test :refer :all]
            [ring.middleware.session :refer :all]
            [ring.middleware.session.store :refer :all]
            [ring.middleware.session.memory :refer [memory-store]]))

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

(defn get-cookies [response]
  (get-in response [:headers "Set-Cookie"]))

(defn is-session-cookie? [c]
  (.contains c "ring-session="))

(defn get-session-cookie [response]
  (first (filter is-session-cookie? (get-cookies response))))

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
    (is (get-session-cookie response))))

(deftest session-delete-outputs-cookie
  (let [store (make-store (constantly {:foo "bar"})
                          (constantly nil)
                          (constantly "deleted"))
        handler (constantly {:session nil})
        handler (wrap-session handler {:store store})
        response (handler {:cookies {"ring-session" {:value "foo:bar"}}})]
    (is (.contains (get-session-cookie response)
                   "ring-session=deleted"))))

(deftest session-cookie-has-attributes
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}})
	handler (wrap-session handler {:store store
                                       :cookie-attrs {:max-age 5 :path "/foo"}})
	response (handler {:cookies {}})
        session-cookie (get-session-cookie response)]
    (is (and (.contains session-cookie "ring-session=foo%3Abar")
             (.contains session-cookie "Max-Age=5")
             (.contains session-cookie "Path=/foo")
             (.contains session-cookie "HttpOnly")))))

(deftest session-does-not-clobber-response-cookies
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}
			     :cookies {"cookie2" "value2"}})
	handler (wrap-session handler {:store store :cookie-attrs {:max-age 5}})
	response (handler {:cookies {}})]
    (is (= (first (remove is-session-cookie? (get-cookies response)))
           "cookie2=value2"))))

(deftest session-root-can-be-set
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}})
	handler (wrap-session handler {:store store, :root "/foo"})
	response (handler {:cookies {}})]
    (is (.contains (get-session-cookie response)
                   "Path=/foo"))))

(deftest session-attrs-can-be-set-per-request
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}
                             :session-cookie-attrs {:max-age 5}})
	handler (wrap-session handler {:store store})
	response (handler {:cookies {}})]
    (is (.contains (get-session-cookie response)
                   "Max-Age=5"))))

(deftest cookie-attrs-override-is-respected
  (let [store (make-store (constantly {})
                          (constantly {})
                          (constantly nil))
	handler (constantly {:session {}})
	handler (wrap-session handler {:store store :cookie-attrs {:http-only false}})
	response (handler {:cookies {}})]
    (is (not (.contains (get-session-cookie response)
                        "HttpOnly")))))

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

(deftest session-request-test
  (is (fn? session-request)))

(deftest session-response-test
  (is (fn? session-response)))

(deftest session-cookie-attrs-change
  (let [a-resp   (atom {:session {:foo "bar"}})
        handler  (wrap-session (fn [req] @a-resp))
        response (handler {})
        sess-key (->> (get-in response [:headers "Set-Cookie"])
                      (first)
                      (re-find #"(?<==)[^;]+"))]
    (is (not (nil? sess-key)))
    (reset! a-resp {:session-cookie-attrs {:max-age 3600}})

    (testing "Session cookie attrs with no active session"
      (is (= (handler {}) {})))

    (testing "Session cookie attrs with active session"
      (let [response (handler {:foo "bar" :cookies {"ring-session" {:value sess-key}}})]
        (is (get-session-cookie response))))))

(deftest session-is-recreated-when-recreate-key-present-in-metadata
  (let [reader  (trace-fn (constantly {}))
        writer  (trace-fn (constantly nil))
        deleter (trace-fn (constantly nil))
        store   (make-store reader writer deleter)
        handler (constantly {:session ^:recreate {:foo "bar"}})
        handler (wrap-session handler {:store store})]
    (handler {:cookies {"ring-session" {:value "test"}}})
    (is (= (trace reader) [["test"]]))
    (is (= (trace writer) [[nil {:foo "bar"}]]))
    (is (= (trace deleter) [["test"]]))
    (testing "session was not written with :recreate metadata intact"
      (let [[[_ written]] (trace writer)]
        (is (not (:recreate (meta written))))))))

(deftest wrap-sesssion-cps-test
  (testing "reading session"
    (let [memory    (atom {"test" {:foo "bar"}})
          store     (memory-store memory)
          handler   (-> (fn [req respond _] (respond (:session req)))
                        (wrap-session {:store store}))
          request   {:cookies {"ring-session" {:value "test"}}}
          response  (promise)
          exception (promise)]
      (handler request response exception)
      (is (= {:foo "bar"} @response))
      (is (= {"test" {:foo "bar"}} @memory))
      (is (not (realized? exception)))))

  (testing "writing session"
    (let [memory    (atom {"test" {}})
          store     (memory-store memory)
          handler   (-> (fn [_ respond _] (respond {:session {:foo "bar"}, :body "foo"}))
                        (wrap-session {:store store}))
          request   {:cookies {"ring-session" {:value "test"}}}
          response  (promise)
          exception (promise)]
      (handler request response exception)
      (is (= "foo" (:body @response)))
      (is (= {"test" {:foo "bar"}} @memory))
      (is (not (realized? exception))))))

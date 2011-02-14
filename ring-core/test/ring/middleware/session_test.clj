(ns ring.middleware.session-test
  (:use clojure.test
        clojure.contrib.mock.test-adapter
        ring.middleware.session
        ring.middleware.session.store))

(defn- make-store [reader writer deleter]
  (reify SessionStore
    (read-session [_ k] (reader k))
    (write-session [_ k s] (writer k s))
    (delete-session [_ k] (deleter k))))

(declare reader writer deleter)

(deftest session-is-read
  (expect [reader  (times 1
                     (has-args [(partial = "test")]
                     (returns {:bar "foo"})))
           writer  (times never)
           deleter (times never)]
    (let [store   (make-store reader writer deleter)
          handler (fn [req]
                    (is (= (req :session) {:bar "foo"}))
                    {})
          handler (wrap-session handler {:store store})]
      (handler {:cookies {"ring-session" {:value "test"}}}))))

(deftest session-is-written
  (expect [reader  (times 1 (returns {}))
           writer  (times 1 (has-args [nil? (partial = {:foo "bar"})]))
           deleter (times never)]
    (let [store   (make-store reader writer deleter)
          handler (constantly {:session {:foo "bar"}})
          handler (wrap-session handler {:store store})]
      (handler {:cookies {}}))))

(deftest session-is-deleted
  (expect [reader  (times 1 (returns {}))
           writer  (times never)
           deleter (times 1 (has-args [(partial = "test")]))]
    (let [store   (make-store reader writer deleter)
          handler (constantly {:session nil})
          handler (wrap-session handler {:store store})]
      (handler {:cookies {"ring-session" {:value "test"}}}))))

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
	handler (wrap-session handler {:store store :cookie-attrs {:max-age 5}})
	response (handler {:cookies {}})]
    (is (= (get-in response [:headers "Set-Cookie"])
	   ["ring-session=foo%3Abar;Path=/;Max-Age=5"]))))

(deftest session-does-not-clobber-response-cookies
  (let [store (make-store (constantly {})
                          (constantly "foo:bar")
                          (constantly nil))
	handler (constantly {:session {:foo "bar"}
			     :cookies {"cookie2" "value2"}})
	handler (wrap-session handler {:store store :cookie-attrs {:max-age 5}})
	response (handler {:cookies {}})]
    (is (= (get-in response [:headers "Set-Cookie"])
	   ["ring-session=foo%3Abar;Path=/;Max-Age=5" "cookie2=value2"]))))

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

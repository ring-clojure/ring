(ns ring.middleware.session-test
  (:use clojure.test
        clojure.contrib.mock
        ring.middleware.session))

(declare reader writer deleter)

(deftest session-is-read
  (expect [reader  (times 1
                     (has-args [(partial = "test")]
                     (returns {:bar "foo"})))
           writer  (times never)
           deleter (times never)]
    (let [store   {:read reader, :write writer, :delete deleter}
          handler (fn [req]
                    (is (= (req :session) {:bar "foo"}))
                    {})
          handler (wrap-session handler {:store store})]
      (handler {:cookies {"ring-session" {:value "test"}}}))))

(deftest session-is-written
  (expect [reader  (times 1 (returns {}))
           writer  (times 1 (has-args [nil? (partial = {:foo "bar"})]))
           deleter (times never)]
    (let [store   {:read reader, :write writer, :delete deleter}
          handler (constantly {:session {:foo "bar"}})
          handler (wrap-session handler {:store store})]
      (handler {:cookies {}}))))

(deftest session-is-deleted
  (expect [reader  (times 1 (returns {}))
           writer  (times never)
           deleter (times 1 (has-args [(partial = "test")]))]
    (let [store   {:read reader, :write writer, :delete deleter}
          handler (constantly {:session nil})
          handler (wrap-session handler {:store store})]
      (handler {:cookies {"ring-session" {:value "test"}}}))))

(deftest session-write-outputs-cookie
  (let [store {:read  (constantly {})
               :write (constantly "foo:bar")}
        handler (constantly {:session {:foo "bar"}})
        handler (wrap-session handler {:store store})
        response (handler {:cookies {}})]
    (is (= (get-in response [:headers "Set-Cookie"])
           ["ring-session=\"foo:bar\""]))))

(deftest session-delete-outputs-cookie
  (let [store {:read   (constantly {:foo "bar"})
               :delete (constantly "deleted")}
        handler (constantly {:session nil})
        handler (wrap-session handler {:store store})
        response (handler {:cookies {"ring-session" {:value "foo:bar"}}})]
    (is (= (get-in response [:headers "Set-Cookie"])
           ["ring-session=\"deleted\""]))))

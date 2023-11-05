(ns ring.test.websocket
  (:require [clojure.test :refer [deftest is testing]]
            [ring.websocket :as ws]
            [ring.websocket.protocols :as wsp]))

(deftest test-request-protocols
  (is (empty? (ws/request-protocols {:headers {}})))
  (is (= ["mqtt"]
         (ws/request-protocols {:headers {"sec-websocket-protocol" "mqtt"}})))
  (is (= ["mqtt" "soap"]
         (ws/request-protocols
          {:headers {"sec-websocket-protocol" "mqtt, soap"}}))))

(deftest test-map-listeners
  (testing "listener methods"
    (let [listener {:on-open (fn [s] [:on-open s])
                    :on-message (fn [s m] [:on-message s m])
                    :on-pong (fn [s d] [:on-pong s d])
                    :on-error (fn [s e] [:on-error s e])
                    :on-close (fn [s c r] [:on-close s c r])}]
      (is (= [:on-open :sock]
             (wsp/on-open listener :sock)))
      (is (= [:on-message :sock "foo"]
             (wsp/on-message listener :sock "foo")))
      (is (= [:on-pong :sock "data"]
             (wsp/on-pong listener :sock "data")))
      (is (= [:on-error :sock "err"]
             (wsp/on-error listener :sock "err")))
      (is (= [:on-close :sock 1000 "closed"]
             (wsp/on-close listener :sock 1000 "closed")))))
  (testing "default on-ping"
    (let [pong? (promise)
          sock  (reify wsp/Socket
                  (-pong [_ _] (deliver pong? true)))]
      (wsp/on-ping {} sock :data)
      (is (true? (deref pong? 1 false)))))
  (testing "custom on-ping"
    (let [pong?    (promise)
          listener {:on-ping (fn [s d] [:on-ping s d])}
          sock     (reify wsp/Socket
                     (-pong [_ _] (deliver pong? true)))]
      (is (= [:on-ping sock :data]
             (wsp/on-ping listener sock :data)))
      (is (false? (deref pong? 1 false))))))

(ns ring.test.websocket
  (:require [clojure.test :refer [deftest is testing]]
            [ring.websocket :as ws]))

(deftest test-request-protocols
  (is (empty? (ws/request-protocols {:headers {}})))
  (is (= ["mqtt"]
         (ws/request-protocols {:headers {"sec-websocket-protocol" "mqtt"}})))
  (is (= ["mqtt" "soap"]
         (ws/request-protocols
          {:headers {"sec-websocket-protocol" "mqtt, soap"}}))))

(ns ring.middleware.session.memory-test
  (:use clojure.test
        ring.middleware.session.memory))

(deftest memory-session-read-not-exist
  (let [store (memory-store)]
    (is ((:read store) "non-existent")
        {})))

(deftest memory-session-create
  (let [store    (memory-store)
        sess-key ((:write store) nil {:foo "bar"})]
    (is (not (nil? sess-key)))
    (is (= ((:read store) sess-key)
           {:foo "bar"}))))

(deftest memory-session-update
  (let [store     (memory-store)
        sess-key  ((:write store) nil {:foo "bar"})
        sess-key* ((:write store) sess-key {:bar "baz"})]
    (is (= sess-key sess-key*))
    (is (= ((:read store) sess-key)
           {:bar "baz"}))))

(deftest memory-session-delete
  (let [store    (memory-store)
        sess-key ((:write store) nil {:foo "bar"})]
    (is (nil? ((:delete store) sess-key)))
    (is (= ((:read store) sess-key)
           {}))))

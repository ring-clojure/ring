(ns ring.middleware.session.cookie-test
  (:use clojure.test
        ring.middleware.session.cookie))

(deftest cookie-session-read-not-exist
  (let [store (cookie-store)]
    (is ((:read store) "non-existent")
        {})))

(deftest cookie-session-create
  (let [store    (cookie-store)
        sess-key ((:write store) nil {:foo "bar"})]
    (is (not (nil? sess-key)))
    (is (= ((:read store) sess-key)
           {:foo "bar"}))))

(deftest cookie-session-update
  (let [store     (cookie-store)
        sess-key  ((:write store) nil {:foo "bar"})
        sess-key* ((:write store) sess-key {:bar "baz"})]
    (is (not (nil? sess-key*)))
    (is (not= sess-key sess-key*))
    (is (= ((:read store) sess-key*)
           {:bar "baz"}))))

(deftest cookie-session-delete
  (let [store     (cookie-store)
        sess-key  ((:write store) nil {:foo "bar"})
        sess-key* ((:delete store) sess-key)]
    (is (not (nil? sess-key*)))
    (is (not= sess-key sess-key*))
    (is (= ((:read store) sess-key*)
           {}))))

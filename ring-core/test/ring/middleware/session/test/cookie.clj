(ns ring.middleware.session.test.cookie
  (:use clojure.test
        [ring.middleware.session store cookie]))

(deftest cookie-session-read-not-exist
  (let [store (cookie-store)]
    (is (read-session store "non-existent")
        {})))

(deftest cookie-session-create
  (let [store    (cookie-store)
        sess-key (write-session store nil {:foo "bar"})]
    (is (not (nil? sess-key)))
    (is (= (read-session store sess-key)
           {:foo "bar"}))))

(deftest cookie-session-update
  (let [store     (cookie-store)
        sess-key  (write-session store nil {:foo "bar"})
        sess-key* (write-session store sess-key {:bar "baz"})]
    (is (not (nil? sess-key*)))
    (is (not= sess-key sess-key*))
    (is (= (read-session store sess-key*)
           {:bar "baz"}))))

(deftest cookie-session-delete
  (let [store     (cookie-store)
        sess-key  (write-session store nil {:foo "bar"})
        sess-key* (delete-session store sess-key)]
    (is (not (nil? sess-key*)))
    (is (not= sess-key sess-key*))
    (is (= (read-session store sess-key*)
           {}))))

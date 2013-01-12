(ns ring.middleware.session.test.cookie
  (:use clojure.test
        [ring.middleware.session store cookie])
  (:require [ring.middleware.session.cookie :as cookie]
            [ring.util.codec :as codec]))

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

(defn seal-code-injection [key code]
  (let [data (#'cookie/encrypt key (.getBytes (str "#=" (pr-str code))))]
    (str (codec/base64-encode data) "--" (#'cookie/hmac key data))))

(deftest cookie-session-code-injection
  (let [secret-key (#'cookie/secure-random-bytes 16)
        store      (cookie-store {:key secret-key})
        session    (seal-code-injection secret-key `(+ 1 1))]
    (is (thrown? RuntimeException (read-session store session)))))

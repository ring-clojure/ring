(ns ring.middleware.session.test.cookie
  (:require [clojure.test :refer :all]
            [ring.middleware.session.store :refer :all]
            [ring.middleware.session.cookie :as cookie :refer [cookie-store]]
            [ring.util.codec :as codec]
            [crypto.random :as random]))

(deftest cookie-session-read-not-exist
  (let [store (cookie-store)]
    (is (nil? (read-session store "non-existent")))))

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
  (let [secret-key (random/bytes 16)
        store      (cookie-store {:key secret-key})
        session    (seal-code-injection secret-key `(+ 1 1))]
    (is (thrown? Exception (read-session store session)))))

(deftest cookie-session-keyword-injection
  (let [store    (cookie-store)
        bad-data {:foo 1 (keyword "bar 3 :baz ") 2}]
    (is (thrown? AssertionError (write-session store nil bad-data)))))

(deftest cookie-session-invalid-key
  (is (thrown-with-msg?
       AssertionError #"the secret key must be exactly 16 bytes"
       (cookie-store {:key (.getBytes "abcd")})))
  (is (thrown-with-msg?
       AssertionError #"the secret key must be exactly 16 bytes"
       (cookie-store {:key (.getBytes "012345678901234567890")}))))

; setup for serializing/deserializing Instant.
(defmethod print-method java.time.Instant [dt out]
  (.write out (str "#foo/instant \"" (.toString dt) "\"")))

(defn parse-instant [x] (java.time.Instant/parse x))

(deftest cookie-session-custom-type
  (let [store    (cookie-store {:readers {'foo/instant #'parse-instant}})
        now      (java.time.Instant/now)
        sess-key (write-session store nil {:foo now})]
    (is (not (nil? sess-key)))
    (is (= (read-session store sess-key)
           {:foo now}))))

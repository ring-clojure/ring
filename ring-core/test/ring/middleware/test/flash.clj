(ns ring.middleware.test.flash
  (:use clojure.test
        ring.middleware.flash))

(deftest flash-is-added-to-session
  (let [message  {:error "Could not save"}
        handler  (wrap-flash (constantly {:flash message}))
        response (handler {:session {}})]
    (is (= (:session response) {:_flash message}))))

(deftest flash-is-retrieved-from-session
  (let [message  {:error "Could not save"}
        handler  (wrap-flash
                   (fn [request]
                     (is (= (:flash request) message))
                     {}))]
    (handler {:session {:_flash message}})))

(deftest flash-is-removed-after-read
  (let [message  {:error "Could not save"}
        handler  (wrap-flash (constantly {:session {:foo "bar"}}))
        response (handler {:session {:_flash message}})]
    (is (nil? (:flash response)))
    (is (= (:session response) {:foo "bar"}))))

(deftest flash-is-removed-after-read-not-touching-session-explicitly
  (let [message  {:error "Could not save"}
        handler  (wrap-flash (constantly {:status 200}))
        response (handler {:session {:_flash message :foo "bar"}})]
    (is (nil? (:flash response)))
    (is (= (:session response) {:foo "bar"}))))

(deftest flash-doesnt-wipe-session
  (let [message  {:error "Could not save"}
        handler  (wrap-flash (constantly {:flash message}))
        response (handler {:session {:foo "bar"}})]
    (is (= (:session response) {:foo "bar", :_flash message}))))

(deftest flash-overwrites-nil-session
  (let [message  {:error "Could not save"}
        handler  (wrap-flash (constantly {:flash message, :session nil}))
        response (handler {:session {:foo "bar"}})]
    (is (= (:session response) {:_flash message}))))

(deftest flash-not-except-on-nil-response
  (let [handler (wrap-flash (constantly nil))]
    (is (nil? (handler {})))))

(deftest flash-request-test
  (is (fn? flash-request)))

(deftest flash-response-test
  (is (fn? flash-response)))

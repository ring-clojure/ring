(ns ring.branch-test
  (:use clj-unit.core ring.branch))

(def wrapped
  (wrap ["/javascripts" "/stylesheets"] (fn [req] [req :static])
        ["/blog"] (fn [req] [req :blog])
        (fn [req] [req :dynamic])))

(deftest "wrap"
  (let [req {:uri "/stylesheets"}]
    (assert= [req :static] (wrapped req)))
  (let [req {:uri "/blog/foo"}]
    (assert= [req :blog] (wrapped req)))
  (let [req {:uri "/foo/blog"}]
    (assert= [req :dynamic] (wrapped req))))

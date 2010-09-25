(ns ring.middleware.nested-params-test
  (:use clojure.test
        ring.middleware.nested-params))

(deftest nested-params-test
  (let [handler (wrap-nested-params :params)]
    (testing "nested parameter maps"
      (are [p r] (= (handler {:params p}) r)
        {"foo" "bar"}      {"foo" "bar"}
        {"x[y]" "z"}       {"x" {"y" "z"}}
        {"a[b][c]" "d"}    {"a" {"b" {"c" "d"}}}
        {"a" "b", "c" "d"} {"a" "b", "c" "d"}))
    (testing "nested parameter lists"
      (are [p r] (= (handler {:params p}) r)
        {"foo[]" "bar"}         {"foo" ["bar"]}
        {"foo[]" ["bar" "baz"]} {"foo" ["bar" "baz"]}
        {"a[x][]" ["b"], "a[x][][y]" "c"} {"a" {"x" ["b" {"y" "c"}]}}
        {"a[][x]" "c", "a[][y]" "d"}      {"a" [{"x" "c"} {"y" "d"}]}))))

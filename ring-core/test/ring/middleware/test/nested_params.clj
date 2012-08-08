(ns ring.middleware.test.nested-params
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
        {"a[][x]" "c", "a[][y]" "d"}      {"a" [{"x" "c"} {"y" "d"}]}))
    (testing "nested parameter path without indexes"
      (are [p r] (= (handler {:params p}) r)
           {"[:foo]" "bar"} {:foo "bar"}
           {"[:x :y]" "z"} {:x {:y "z"}}
           {"[:a :b :c]" "d"} {:a {:b {:c "d"}}}
           {"[:a]" "b", "[:c]" "d"} {:a "b", :c "d"}
           ))
    (testing "nested parameter path with indexes"
      (are [p r] (= (handler {:params p}) r)
           {"[:parents 0 :id]" "1"} {:parents [{:id "1"}]}
           {"[:parents 1 :id]" "1"} {:parents [nil {:id "1"}]}
           {"[:parents 0 :children 0 :grandchildren 0 :id]" "1"} {:parents [{:children [{:grandchildren [{:id "1"}]}]}]}
           {"[:parents 0 :children 0 :grandchildren 0 :id]" "1"
            "[:parents 0 :children 0 :grandchildren 1 :id]" "2"
            "[:parents 0 :children 1 :grandchildren 0 :id]" "3"
            "[:parents 0 :children 0 :id]" "4"
            "[:parents 0 :children 1 :id]" "5"
            "[:parents 0 :id]" "6"}
           {:parents
            [{:id "6"
              :children [{:id "4"
                          :grandchildren [{:id "1"} {:id "2"}]}
                         {:id "5"
                          :grandchildren [{:id "3"}]}]}]}))))

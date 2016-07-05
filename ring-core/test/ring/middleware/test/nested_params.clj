(ns ring.middleware.test.nested-params
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [ring.middleware.nested-params :refer :all]))

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
        {"foo[]" ["bar" "baz"]} {"foo" ["bar" "baz"]})
      (let [params (handler {:params {"a[x][]" ["b"], "a[x][][y]" "c"}})]
        (is (= (keys params) ["a"]))
        (is (= (keys (params "a")) ["x"]))
        (is (= (set (get-in params ["a" "x"])) #{"b" {"y" "c"}})))
      (let [params (handler {:params {"a[][x]" "c", "a[][y]" "d"}})]
        (is (= (keys params) ["a"]))
        (is (= (set (params "a")) #{{"x" "c"} {"y" "d"}}))))
    (testing "duplicate parameters"
      (are [p r] (= (handler {:params p}) r)
        {"a" ["b" "c"]}    {"a" ["b" "c"]}
        {"a[b]" ["c" "d"]} {"a" {"b" ["c" "d"]}}))
    (testing "parameters with newlines"
      (are [p r] (= (handler {:params p}) r)
        {"foo\nbar" "baz"} {"foo\nbar" "baz"}))
    (testing "empty string"
      (is (= {"foo" {"bar" "baz"}}
             (handler {:params (sorted-map "foo" ""
                                           "foo[bar]" "baz")}))))
    (testing "double nested empty string"
      (is (= {"foo" {"bar" {"baz" "bam"}}}
             (handler {:params (sorted-map "foo[bar]" ""
                                           "foo[bar][baz]" "bam")}))))
    (testing "parameters are already nested"
      (is (= {"foo" [["bar" "baz"] ["asdf" "zxcv"]]}
             (handler {:params {"foo" [["bar" "baz"] ["asdf" "zxcv"]]}}))))
    (testing "pre-nested vector of maps"
      (is (= {"foo" [{"bar" "baz"} {"asdf" "zxcv"}]}
             (handler {:params {"foo" [{"bar" "baz"} {"asdf" "zxcv"}]}}))))
    (testing "pre-nested map"
      (is (= {"foo" [{"bar" "baz" "asdf" "zxcv"}]}
             (handler {:params {"foo" [{"bar" "baz" "asdf" "zxcv"}]}}))))
    (testing "double-nested map"
      (is (= {"foo" {"key" {"bar" "baz" "asdf" "zxcv"}}}
             (handler {:params {"foo" {"key" {"bar" "baz" "asdf" "zxcv"}}}}))))))

(deftest nested-params-test-with-options
  (let [handler (wrap-nested-params :params
                                    {:key-parser #(string/split % #"\.")})]
    (testing ":key-parser option"
      (are [p r] (= (handler {:params p}) r)
        {"foo" "bar"}      {"foo" "bar"}
        {"x.y" "z"}       {"x" {"y" "z"}}
        {"a.b.c" "d"}    {"a" {"b" {"c" "d"}}}
        {"a" "b", "c" "d"} {"a" "b", "c" "d"}))))

(deftest wrap-nested-params-cps-test
  (let [handler   (wrap-nested-params (fn [req respond _] (respond (:params req))))
        request   {:params {"x[y]" "z"}}
        response  (promise)
        exception (promise)]
    (handler request response exception)
    (is (= {"x" {"y" "z"}} @response))
    (is (not (realized? exception)))))

(deftest nested-params-request-test
  (is (fn? nested-params-request)))

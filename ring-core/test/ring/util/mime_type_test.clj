(ns ring.util.mime-type-test
  (:use clojure.test
        ring.util.mime-type))

(deftest ext-mime-type-test
  (testing "default mime types"
    (are [f m] (= (ext-mime-type f) m)
      "foo.txt"  "text/plain"
      "foo.html" "text/html"
      "foo.png"  "image/png"))
  (testing "custom mime types"
    (is (= (ext-mime-type "foo.bar" {"bar" "application/bar"})
           "application/bar"))
    (is (= (ext-mime-type "foo.txt" {"txt" "application/text"})
           "application/text"))))

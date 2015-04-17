(ns ring.util.test.time
  (:require [clojure.test :refer :all]
            [ring.util.time :refer :all]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-date]]))

(deftest test-parse-date
  (are [x y] (= (parse-date x) (to-date y))
    "Sun, 06 Nov 1994 08:49:37 GMT"   (date-time 1994 11 6 8 49 37)
    "Sunday, 06-Nov-94 08:49:37 GMT"  (date-time 1994 11 6 8 49 37)
    "Sun Nov  6 08:49:37 1994"        (date-time 1994 11 6 8 49 37)
    "'Sun, 06 Nov 1994 08:49:37 GMT'" (date-time 1994 11 6 8 49 37)))

(ns ring.middleware.test.locale
  (:use clojure.test
        ring.middleware.locale))


(defn assert-extracted-locale [expected-locale test-request options]
  (is (= expected-locale
         ((wrap-locale (fn [request] (:locale request))
             :locale-augmenters (:locale-augmenters options)
             :accepted-locales (:accepted-locales options)
             :default-locale (:default-locale options))
          test-request))))


(deftest gets-locale-from-locale-augmenter-passed-in-through-the-options
  (let [locale-augmenter (fn [_ _] {:locale "en_US"})
        options {:locale-augmenters [locale-augmenter]}]
    (assert-extracted-locale "en_US" {} options)))

(deftest can-restrict-to-a-list-of-accepted-locales
  (let [locale-augmenter (fn [_ _] {:locale "en_CA"})
        options {:locale-augmenters [locale-augmenter]
                 :accepted-locales #{"fr_CA"}}]
    (assert-extracted-locale nil {} options)))

(deftest uses-the-locale-augmenter-that-is-highest-in-priority
  (let [first-locale-augmenter (fn [_ _] {:locale "es_LA"})
        second-locale-augmenter (fn [_ _] {:locale "en_US"})
        options {:locale-augmenters [first-locale-augmenter second-locale-augmenter]
                 :accepted-locales #{"es_LA" "en_US"}}]
    (assert-extracted-locale "es_LA" {} options)))

(deftest ignores-locale-extractors-with-no-locale-data
  (let [first-locale-augmenter (fn [_ _] {})
        second-locale-augmenter (fn [_ _] {:locale "en_US"})
        options {:locale-augmenters [first-locale-augmenter second-locale-augmenter]}]
    (assert-extracted-locale "en_US" {} options)))

(deftest uses-the-default-locale-if-the-locale-extractors-dont-return-any-locale-data
  (let [locale-augmenter (fn [_ _] {})
        options {:locale-augmenters [locale-augmenter]
                 :default-locale "fr_CA"}]
    (assert-extracted-locale "fr_CA" {} options)))

(deftest has-a-nil-locale-if-the-locale-extractors-dont-return-any-locale-data-and-there-is-no-default-locale
  (let [locale-augmenter (fn [_ _] {})
        options {:locale-augmenters [locale-augmenter]}]
    (assert-extracted-locale nil {} options)))
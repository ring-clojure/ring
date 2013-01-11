(ns ring.middleware.test.locale
  (:use clojure.test
        ring.middleware.locale))


(defn assert-extracted-locale [expected-locale test-request options]
  (is (= expected-locale
         ((wrap-locale (fn [request] (:locale request))
             :locale-augmenters (:locale-augmenters options)
             :accepted-locales (:accepted-locales options)
             :default-locale (:default-locale options)
             :locale-cookie-name (:locale-cookie-name options))
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


(deftest provides-a-locale-augmenter-to-pull-locale-from-uri
  (let [request {:uri "/en/index.html"}
        options {:accepted-locales #{"en"}
                 :locale-augmenters [uri]}]
    (assert-extracted-locale "en" request options)))

(deftest will-assume-first-item-in-uri-is-a-locale-if-accepted-locales-arent-specified
  (let [request {:uri "/index.html"}
        options {:locale-augmenters [uri]}]
    (assert-extracted-locale "index.html" request options)))

(deftest scrubs-locale-from-uri-if-an-accepted-locale-is-returned
  (let [request {:uri "/en/index.html"}
        options {}]
    (is (= "/index.html"
         ((wrap-locale (fn [request] (:uri request))
             :locale-augmenters [uri])
          request)))))


(deftest provides-a-function-to-pull-locale-from-a-cookie
  (let [request {:cookies {"locale" {:value "es_LA"}}}
        options {:locale-augmenters [cookie]}]
    (assert-extracted-locale "es_LA" request options)))

(deftest parses-the-cookies-from-the-header-if-it-needs-to
  (let [request {:headers {"cookie" "a=b; c=d,locale=en_US"}}
        options {:locale-augmenters [cookie]}]
    (assert-extracted-locale "en_US" request options)))

(deftest allows-to-pull-locale-from-a-cookie-other-than-the-locale-cookie
  (let [request {:headers {"cookie" "a=b; custom_cookie=piglatin,e=en_US"}}
        options {:locale-cookie-name "custom_cookie"
                 :accepted-locales #{"en_US" "piglatin"}
                 :locale-augmenters [cookie]}]
    (assert-extracted-locale "piglatin" request options)))

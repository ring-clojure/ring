(ns ring.middleware.test.locale
  (:use clojure.test
        ring.middleware.locale))


(defn assert-extracted-locale [expected-locale test-request options]
  (is (= expected-locale
         ((wrap-locale (fn [request] (:locale request))
             :locale-augmenters (:locale-augmenters options)
             :accepted-locales (:accepted-locales options)
             :default-locale (:default-locale options)
             :locale-cookie-name (:locale-cookie-name options)
             :locale-param-name (:locale-param-name options))
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


(deftest provides-a-function-to-pull-locale-from-the-query-string
  (let [request {:query-params {"locale" "en_PI"}}
        options {:locale-augmenters [query-param]}]
    (assert-extracted-locale "en_PI" request options)))

(deftest parses-the-query-params-if-it-needs-to
  (let [request {:query-string "locale=fr_CA"}
        options {:locale-augmenters [query-param]}]
    (assert-extracted-locale "fr_CA" request options)))

(deftest the-default-locale-param-name-is-locale
  (let [request {:query-params {"some_param" "pirate"}}
        options {:locale-param-name "some_param"
                 :locale-augmenters [query-param]}]
    (assert-extracted-locale "pirate" request options)))



(deftest provides-a-function-to-pull-locale-from-the-accept-language-header
  (let [request {:headers {"accept-language" "en_US"}}
        options {:locale-augmenters [accept-header]}]
    (assert-extracted-locale "en_US" request options)))

(deftest picks-the-accepted-locale-with-the-highest-q-score
  (let [request {:headers {"accept-language" "en_US,en;q=0.9,ja;q=0.8,fr;q=0.7,de;q=0.6,es;q=0.5"}}
        options {:accepted-locales #{"ja" "de" "es"}
                 :locale-augmenters [accept-header]}]
    (assert-extracted-locale "ja" request options)))

(deftest returns-nil-if-there-is-no-accept-language-header
  (let [request {:headers {}}
        options {:locale-augmenters [accept-header]}]
    (assert-extracted-locale nil request options)))

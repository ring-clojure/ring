(defproject ring/ring-servlet "1.2.0-beta1"
  :description "Ring servlet utilities."
  :url "https://github.com/ring-clojure/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[ring/ring-core "1.2.0-beta1"]
                 [javax.servlet/servlet-api "2.5"]]
  :profiles
  {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}})

(defproject ring/ring-servlet "1.2.2"
  :description "Ring servlet utilities."
  :url "https://github.com/ring-clojure/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[ring/ring-core "1.2.2"]]
  :profiles
  {:provided {:dependencies [[javax.servlet/servlet-api "2.5"]]}
   :dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})

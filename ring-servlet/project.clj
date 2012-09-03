(defproject ring/ring-servlet "1.2.0-SNAPSHOT"
  :description "Ring servlet utilities."
  :url "https://github.com/ring-clojure/ring"
  :dependencies [[ring/ring-core "1.2.0-SNAPSHOT"]
                 [javax.servlet/servlet-api "2.5"]]
  :profiles
  {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}})

(defproject ring/ring-servlet "1.6.0"
  :description "Ring servlet utilities."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.6.0"]]
  :profiles
  {:provided {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]}
   :dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})

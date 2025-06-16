(defproject ring/ring-servlet "1.14.1"
  :description "Ring servlet utilities."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ring/ring-core "1.14.1"]]
  :aliases {"test-all" ["with-profile" "default:+1.10:+1.11:+1.12" "test"]}
  :profiles
  {:provided {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]}
   :dev  {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
   :1.11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
   :1.12 {:dependencies [[org.clojure/clojure "1.12.1"]]}})

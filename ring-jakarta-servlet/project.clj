(defproject org.ring-clojure/ring-jakarta-servlet "1.15.0-RC1"
  :description "Ring Jakarta servlet utilities."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ring/ring-core "1.15.0-RC1"]]
  :aliases {"test-all" ["with-profile" "default:+1.10:+1.11:+1.12" "test"]}
  :profiles
  {:provided {:dependencies [[jakarta.servlet/jakarta.servlet-api "5.0.0"]]}
   :dev  {:dependencies [[jakarta.servlet/jakarta.servlet-api "5.0.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
   :1.11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
   :1.12 {:dependencies [[org.clojure/clojure "1.12.1"]]}})

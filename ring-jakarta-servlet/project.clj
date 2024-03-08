(defproject org.ring-clojure/ring-jakarta-servlet "1.11.0"
  :description "Ring Jakarta servlet utilities."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.11.0"]]
  :aliases {"test-all" ["with-profile" "default:+1.8:+1.9:+1.10:+1.11:+1.12" "test"]}
  :profiles
  {:provided {:dependencies [[jakarta.servlet/jakarta.servlet-api "5.0.0"]]}
   :dev  {:dependencies [[jakarta.servlet/jakarta.servlet-api "5.0.0"]]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
   :1.11 {:dependencies [[org.clojure/clojure "1.11.2"]]}
   :1.12 {:dependencies [[org.clojure/clojure "1.12.0-alpha9"]]}})

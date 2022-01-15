(defproject ring/ring-devel "1.9.5"
  :description "Ring development and debugging libraries."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.9.5"]
                 [hiccup "1.0.5"]
                 [clj-stacktrace "0.2.8"]
                 [ns-tracker "0.4.0"]]
  :aliases {"test-all" ["with-profile" "default:+1.8:+1.9:+1.10" "test"]}
  :profiles
  {:1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}})

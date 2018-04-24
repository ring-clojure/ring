(defproject ring/ring-devel "1.6.3"
  :description "Ring development and debugging libraries."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.6.3"]
                 [hiccup "1.0.5"]
                 [clj-stacktrace "0.2.8"]
                 [ns-tracker "0.3.1"]]
  :aliases {"test-all" ["with-profile" "default:+1.6:+1.7:+1.8:+1.9" "test"]}
  :profiles
  {:1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9 {:dependencies [[org.clojure/clojure "1.8.0"]]}})

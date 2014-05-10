(defproject ring/ring-devel "1.3.0-beta2"
  :description "Ring development and debugging libraries."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[ring/ring-core "1.3.0-beta2"]
                 [hiccup "1.0.3"]
                 [clj-stacktrace "0.2.7"]
                 [ns-tracker "0.2.2"]]
  :profiles
  {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}})

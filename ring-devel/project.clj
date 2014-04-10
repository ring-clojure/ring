(defproject ring/ring-devel "1.2.2"
  :description "Ring development and debugging libraries."
  :url "https://github.com/ring-clojure/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[ring/ring-core "1.2.2"]
                 [hiccup "1.0.3"]
                 [clj-stacktrace "0.2.7"]
                 [ns-tracker "0.2.2"]]
  :profiles
  {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})

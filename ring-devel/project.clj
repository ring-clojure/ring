(defproject ring/ring-devel "1.2.0-SNAPSHOT"
  :description "Ring development and debugging libraries."
  :url "https://github.com/ring-clojure/ring"
  :dependencies [[ring/ring-core "1.2.0-SNAPSHOT"]
                 [hiccup "1.0.0"]
                 [clj-stacktrace "0.2.5"]
                 [ns-tracker "0.2.1"]]
  :profiles
  {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.0-RC1"]]}})

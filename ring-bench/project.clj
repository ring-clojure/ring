(defproject ring/ring-bench "1.8.2"
  :description "Ring core libraries."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [jmh-clojure "0.2.1"]
                 [ring/ring-jetty-adapter "1.8.2"]
                 [ring/ring-servlet "1.8.2"]]
  :jvm-opts {}
  :main ring.bench.servlet)

(defproject ring/ring-jetty-adapter "1.2.2"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[ring/ring-core "1.2.2"]
                 [ring/ring-servlet "1.2.2"]
                 [org.eclipse.jetty/jetty-server "7.6.8.v20121106"]]
  :profiles
  {:dev {:dependencies [[clj-http "0.6.4"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}})

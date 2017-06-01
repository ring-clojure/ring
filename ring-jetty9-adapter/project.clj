(defproject ring/ring-jetty9-adapter "0.1.0-SNAPSHOT"
  :description "Ring Jetty 9 adapter."
  :url "https://github.com/ring-clojure/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[ring/ring-core "1.2.1"]
                 [ring/ring-servlet "1.2.1"]
                 [org.eclipse.jetty/jetty-server "9.1.0.RC0"]]
  :profiles
  {:dev {:dependencies [[clj-http "0.6.4"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})

(defproject ring/ring-jetty-adapter "1.2.0-SNAPSHOT"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :dependencies [[ring/ring-core "1.2.0-SNAPSHOT"]
                 [ring/ring-servlet "1.2.0-SNAPSHOT"]
                 [org.eclipse.jetty/jetty-server "7.6.1.v20120215"]]
  :profiles
  {:dev {:dependencies [[clj-http "0.3.2"]]}})

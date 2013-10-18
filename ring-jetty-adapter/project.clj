(defproject ring/ring-jetty-adapter "1.2.0"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[ring/ring-core "1.2.0"]
                 [ring/ring-servlet "1.2.0"]
                 [org.eclipse.jetty/jetty-server "7.6.13.v20130916"]]
  :profiles
  {:dev {:dependencies [[clj-http "0.6.4"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})

(defproject ring/ring-jetty-adapter "1.4.0-beta1"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.4.0-beta1"]
                 [ring/ring-servlet "1.4.0-beta1"]
                 [org.eclipse.jetty/jetty-server "7.6.13.v20130916"]]
  :profiles
  {:dev {:dependencies [[clj-http "1.1.2"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}})

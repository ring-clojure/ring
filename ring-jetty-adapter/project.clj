(defproject ring/ring-jetty-adapter "1.5.1"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.5.1"]
                 [ring/ring-servlet "1.5.1"]
                 [org.eclipse.jetty/jetty-server "9.2.10.v20150310"]]
  :profiles
  {:dev {:dependencies [[clj-http "2.2.0"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})

(defproject ring/ring-jetty-adapter "1.6.0-beta7"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.6.0-beta7"]
                 [ring/ring-servlet "1.6.0-beta7"]
                 [org.eclipse.jetty/jetty-server "9.2.17.v20160517"]]
  :profiles
  {:dev {:dependencies [[clj-http "2.2.0"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})

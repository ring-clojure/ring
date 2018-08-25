(defproject ring/ring-jetty-adapter "1.7.0-RC2"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.7.0-RC2"]
                 [ring/ring-servlet "1.7.0-RC2"]
                 [org.eclipse.jetty/jetty-server "9.2.24.v20180105"]]
  :aliases {"test-all" ["with-profile" "default:+1.8:+1.9" "test"]}
  :profiles
  {:dev {:dependencies [[clj-http "2.2.0"]]
         :jvm-opts ["-Dorg.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT=500"]}
   :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}})

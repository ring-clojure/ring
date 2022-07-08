(defproject ring/ring-jetty-adapter "1.9.5"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-servlet "1.9.5"]
                 [org.eclipse.jetty/jetty-server "9.4.48.v20220622"]]
  :aliases {"test-all" ["with-profile" "default:+1.8:+1.9:+1.10" "test"]}
  :profiles
  {:dev  {:dependencies [[clj-http "3.12.3"]
                         [less-awful-ssl "1.0.6"]]
          :jvm-opts ["-Dorg.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT=500"]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}})

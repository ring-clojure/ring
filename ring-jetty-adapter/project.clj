(defproject ring/ring-jetty-adapter "1.11.0"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.11.0"]
                 [org.ring-clojure/ring-jakarta-servlet "1.11.0"]
                 [org.eclipse.jetty/jetty-server "11.0.18"]
                 [org.eclipse.jetty.websocket/websocket-jetty-server "11.0.18"]]
  :aliases {"test-all" ["with-profile" "default:+1.8:+1.9:+1.10:+1.11" "test"]}
  :profiles
  {:dev  {:dependencies [[clj-http "3.12.3"]
                         [less-awful-ssl "1.0.6"]
                         [hato "0.9.0"]
                         [org.slf4j/slf4j-simple "2.0.9"]]
          :jvm-opts ["-Dorg.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT=500"]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
   :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}})

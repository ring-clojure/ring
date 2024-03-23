(defproject ring/ring-jetty-adapter "1.12.0"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ring/ring-core "1.12.0"]
                 [org.ring-clojure/ring-jakarta-servlet "1.12.0"]
                 [org.eclipse.jetty/jetty-server "11.0.20"]
                 [org.eclipse.jetty.websocket/websocket-jetty-server "11.0.20"]]
  :aliases {"test-all" ["with-profile" "default:+1.10:+1.11:+1.12" "test"]}
  :profiles
  {:dev  {:dependencies [[clj-http "3.12.3"]
                         [less-awful-ssl "1.0.6"]
                         [hato "0.9.0"]]
          :jvm-opts ["-Dorg.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT=500"]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
   :1.11 {:dependencies [[org.clojure/clojure "1.11.2"]]}
   :1.12 {:dependencies [[org.clojure/clojure "1.12.0-alpha9"]]}})

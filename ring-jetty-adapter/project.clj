(defproject ring/ring-jetty-adapter "2.0.0-alpha1"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ring/ring-core "2.0.0-alpha1"]
                 [ring/ring-servlet "2.0.0-alpha1"]
                 [org.eclipse.jetty/jetty-server "9.4.44.v20210927"]]
  :aliases {"test-all" ["with-profile" "default:+1.10" "test"]}
  :profiles
  {:dev  {:dependencies [[clj-http "3.12.3"]
                         [less-awful-ssl "1.0.6"]]
          :jvm-opts ["-Dorg.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT=500"]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}})

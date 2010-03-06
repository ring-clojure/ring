(defproject ring/ring-jetty-adapter "0.2.0-SNAPSHOT"
  :description "Ring Jetty adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.2.0-SNAPSHOT"]
                 [ring/ring-servlet "0.2.0-SNAPSHOT"]
                 [org.mortbay.jetty/jetty "6.1.14"]
                 [org.mortbay.jetty/jetty-util "6.1.14"]]
  :namespaces :all
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]])

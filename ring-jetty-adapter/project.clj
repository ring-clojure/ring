(defproject ring/ring-jetty-adapter "1.1.0-SNAPSHOT"
  :description "Ring Jetty adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "1.1.0-SNAPSHOT"]
                 [ring/ring-servlet "1.1.0-SNAPSHOT"]
                 [org.mortbay.jetty/jetty "6.1.25"]
                 [org.mortbay.jetty/jetty-util "6.1.25"]]
  :dev-dependencies [[clj-http "0.3.2"]])

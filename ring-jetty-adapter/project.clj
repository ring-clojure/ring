(defproject ring/ring-jetty-adapter "0.3.2"
  :description "Ring Jetty adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.3.2"]
                 [ring/ring-servlet "0.3.2"]
                 [org.mortbay.jetty/jetty "6.1.14"]
                 [org.mortbay.jetty/jetty-util "6.1.14"]]
  :dev-dependencies [[clj-http "0.1.1"]])

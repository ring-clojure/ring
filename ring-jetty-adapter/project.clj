(defproject ring/ring-jetty-adapter "0.3.6"
  :description "Ring Jetty adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.3.6"]
                 [ring/ring-servlet "0.3.6"]
                 [org.mortbay.jetty/jetty "6.1.26"]
                 [org.mortbay.jetty/jetty-util "6.1.26"]]
  :dev-dependencies [[clj-http "0.1.1"]])

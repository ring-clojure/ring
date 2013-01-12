(defproject ring/ring-jetty-adapter "1.0.3"
  :description "Ring Jetty adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "1.0.3"]
                 [ring/ring-servlet "1.0.3"]
                 [org.mortbay.jetty/jetty "6.1.25"]
                 [org.mortbay.jetty/jetty-util "6.1.25"]]
  :dev-dependencies [[clj-http "0.1.3"]])

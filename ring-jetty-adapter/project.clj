(defproject ring/ring-jetty-adapter "1.1.0-beta3"
  :description "Ring Jetty adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "1.1.0-beta3"]
                 [ring/ring-servlet "1.1.0-beta3"]
                 [org.eclipse.jetty/jetty-server "7.6.1.v20120215"]]
  :profiles
  {:dev {:dependencies [[clj-http "0.3.2"]]}})

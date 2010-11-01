(defproject ring/ring-jetty-async-adapter "0.3.3-SNAPSHOT"
  :description "Ring Jetty adapter, with async HTTP and WebSockets support."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.3.3" :exclusions [javax.servlet/servlet-api]]
                 [ring/ring-servlet "0.3.3" :exclusions [javax.servlet/servlet-api]]
                 [org.eclipse.jetty/jetty-server "8.0.0.M1"]
                 [org.eclipse.jetty/jetty-websocket "8.0.0.M1"]]
  :dev-dependencies [[clj-http "0.1.1"]])

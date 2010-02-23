(defproject ring-jetty-adapter "0.2.0-SNAPSHOT"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.2.0-SNAPSHOT"] 
                 [ring/ring-servlet "0.2.0-SNAPSHOT"] 
                 [org.mortbay.jetty/jetty "6.1.14"] 
                 [org.mortbay.jetty/jetty-util "6.1.14"]]
  :repositories [["mvnrepository" "http://mvnrepository.com/"]
                 ["clojure-releases" "http://build.clojure.org/releases"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]])
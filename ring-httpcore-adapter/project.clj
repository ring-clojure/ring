(defproject ring-httpcore-adapter "0.2.0-SNAPSHOT"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.2.0-SNAPSHOT"] 
                 [org.apache.httpcomponents/httpcore "4.0.1"] 
                 [org.apache.httpcomponents/httpcore-nio "4.0.1"]]
  :repositories [["mvnrepository" "http://mvnrepository.com/"]
                 ["clojure-releases" "http://build.clojure.org/releases"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]])

(defproject ring "1.1.0-SNAPSHOT"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies
    [[ring/ring-core "1.1.0-SNAPSHOT"]
     [ring/ring-devel "1.1.0-SNAPSHOT"]
     [ring/ring-jetty-adapter "1.1.0-SNAPSHOT"]
     [ring/ring-servlet "1.1.0-SNAPSHOT"]]
  :dev-dependencies
    [[lein-sub "0.1.1"]
     [codox "0.4.1"]]
  :sub
    ["ring-core"
     "ring-devel"
     "ring-jetty-adapter"
     "ring-servlet"]
  :codox
    {:sources ["ring-core/src"
               "ring-devel/src"
               "ring-jetty-adapter/src"
               "ring-servlet/src"]})

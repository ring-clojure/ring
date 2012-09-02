(defproject ring "1.2.0-SNAPSHOT"
  :description "A Clojure web applications library."
  :url "https://github.com/ring-clojure/ring"
  :dependencies
    [[ring/ring-core "1.2.0-SNAPSHOT"]
     [ring/ring-devel "1.2.0-SNAPSHOT"]
     [ring/ring-jetty-adapter "1.2.0-SNAPSHOT"]
     [ring/ring-servlet "1.2.0-SNAPSHOT"]]
  :plugins
    [[lein-sub "0.2.0"]
     [codox "0.6.1"]]
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

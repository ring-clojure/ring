(defproject ring "1.0.1"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies
    [[ring/ring-core "1.0.1"]
     [ring/ring-devel "1.0.1"]
     [ring/ring-jetty-adapter "1.0.1"]
     [ring/ring-servlet "1.0.1"]]
  :dev-dependencies
    [[lein-sub "0.1.1"]
     [codox "0.3.0"]]
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

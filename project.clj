(defproject ring "1.1.0-beta1"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies
    [[ring/ring-core "1.1.0-beta1"]
     [ring/ring-devel "1.1.0-beta1"]
     [ring/ring-jetty-adapter "1.1.0-beta1"]
     [ring/ring-servlet "1.1.0-beta1"]]
  :plugins
    [[lein-sub "0.2.0"]
     [codox "0.5.0"]]
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

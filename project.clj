(defproject ring "1.1.5"
  :description "A Clojure web applications library."
  :url "https://github.com/ring-clojure/ring"
  :dependencies
    [[ring/ring-core "1.1.5"]
     [ring/ring-devel "1.1.5"]
     [ring/ring-jetty-adapter "1.1.5"]
     [ring/ring-servlet "1.1.5"]]
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

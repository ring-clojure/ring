(defproject ring "1.0.0-RC5"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies
    [[ring/ring-core "1.0.0-RC5"]
     [ring/ring-devel "1.0.0-RC5"]
     [ring/ring-jetty-adapter "1.0.0-RC5"]
     [ring/ring-servlet "1.0.0-RC5"]]
  :dev-dependencies
    [[codox "0.3.0"]]
  :codox
    {:sources ["ring-core/src"
               "ring-devel/src"
               "ring-jetty-adapter/src"
               "ring-servlet/src"]})

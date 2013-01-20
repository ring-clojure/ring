(defproject ring "1.1.8"
  :description "A Clojure web applications library."
  :url "https://github.com/ring-clojure/ring"
  :dependencies
    [[ring/ring-core "1.1.8"]
     [ring/ring-devel "1.1.8"]
     [ring/ring-jetty-adapter "1.1.8"]
     [ring/ring-servlet "1.1.8"]]
  :plugins
    [[lein-sub "0.2.0"]
     [codox "0.6.3"]]
  :sub
    ["ring-core"
     "ring-devel"
     "ring-jetty-adapter"
     "ring-servlet"]
  :codox
    {:src-dir-uri "http://github.com/ring-clojure/ring/blob/1.1.8"
     :src-linenum-anchor-prefix "L"
     :sources ["ring-core/src"
               "ring-devel/src"
               "ring-jetty-adapter/src"
               "ring-servlet/src"]})

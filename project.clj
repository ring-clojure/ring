(defproject ring "1.4.0-beta2"
  :description "A Clojure web applications library."
  :url "https://github.com/ring-clojure/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.4.0-beta2"]
                 [ring/ring-devel "1.4.0-beta2"]
                 [ring/ring-jetty-adapter "1.4.0-beta2"]
                 [ring/ring-servlet "1.4.0-beta2"]]
  :plugins [[lein-sub "0.2.4"]
            [codox "0.8.11"]]
  :sub ["ring-core"
        "ring-devel"
        "ring-jetty-adapter"
        "ring-servlet"]
  :codox {:src-dir-uri "http://github.com/ring-clojure/ring/blob/1.3.2/"
          :src-linenum-anchor-prefix "L"
          :sources ["ring-core/src"
                    "ring-devel/src"
                    "ring-jetty-adapter/src"
                    "ring-servlet/src"]})

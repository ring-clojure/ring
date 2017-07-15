(defproject ring "1.6.2"
  :description "A Clojure web applications library."
  :url "https://github.com/ring-clojure/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-devel "1.6.2"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [ring/ring-servlet "1.6.2"]]
  :plugins [[lein-sub "0.2.4"]
            [lein-codox "0.10.3"]]
  :sub ["ring-core"
        "ring-devel"
        "ring-jetty-adapter"
        "ring-servlet"]
  :codox {:output-path "codox"
          :source-uri "http://github.com/ring-clojure/ring/blob/{version}/{filepath}#L{line}"
          :source-paths ["ring-core/src"
                         "ring-devel/src"
                         "ring-jetty-adapter/src"
                         "ring-servlet/src"]})

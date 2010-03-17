(defproject ring/ring-httpcore-adapter "0.2.0-RC2"
  :description "Ring HttpCore adapter."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.2.0-RC2"]
                 [org.apache.httpcomponents/httpcore "4.0.1"]
                 [org.apache.httpcomponents/httpcore-nio "4.0.1"]]
  :dev-dependencies [[org.clojars.mmcgrana/lein-clojars "0.5.0"]])

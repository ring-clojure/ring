(defproject ring "0.1.1-SNAPSHOT"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]
                 [commons-io "1.4"]
                 [org.mortbay.jetty/jetty "6.1.14"]
                 [org.mortbay.jetty/jetty-util "6.1.14"]
                 [org.mortbay.jetty/servlet-api-2.5 "6.1.14"]
                 [httpcomponents-httpcore/httpcore "4.0-alpha6"]
                 [httpcomponents-httpcore/httpcore-nio "4.0-alpha6"]
                 [clj-html "0.1.0-SNAPSHOT"]
                 [clj-stacktrace "0.1.0-SNAPSHOT"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [clj-unit "0.1.0-SNAPSHOT"]])

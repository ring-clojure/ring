(defproject ring "0.2.0-SNAPSHOT"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[ring/ring-core "0.2.0-SNAPSHOT"]
                 [ring/ring-devel "0.2.0-SNAPSHOT"]
                 [ring/ring-httpcore-adapter "0.2.0-SNAPSHOT"]
                 [ring/ring-jetty-adapter "0.2.0-SNAPSHOT"]
                 [ring/ring-servlet "0.2.0-SNAPSHOT"]]
  :dev-dependencies [[autodoc "0.7.0"]]
  :autodoc
    {:name "Ring"
     :description "A Clojure web applications library"
     :copyright "Copyright 2009-2010 Mark McGranaghan"
     :root "."
     :source-path ""
     :output-path "autodoc-output"
     :namespaces-to-document ["ring"]
     :trim-prefix "ring."
     :load-except-list [#"/example/" #"/test/" #"project\.clj"]})

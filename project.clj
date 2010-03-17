(defproject ring "0.2.0-RC1"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dev-dependencies [[autodoc "0.7.0"]]
  :autodoc
    {:name "Ring"
     :description "A Clojure web applications library"
     :copyright "Copyright 2009-2010 Mark McGranaghan"
     :root "."
     :output-path "autodoc-output"
     :namespaces-to-document ["ring"]
     :trim-prefix "ring."
     :load-except-list [#"/example/" #"/test/" #"project\.clj"]})

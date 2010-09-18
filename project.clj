(defproject ring "0.3.0"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies
    [[ring/ring-core "0.3.0"]
     [ring/ring-devel "0.3.0"]
     [ring/ring-httpcore-adapter "0.3.0"]
     [ring/ring-jetty-adapter "0.3.0"]
     [ring/ring-servlet "0.3.0"]]
  :dev-dependencies
    [[autodoc "0.7.1"]
     [lein-clojars "0.6.0"]]
  :autodoc
    {:name "Ring"
     :description "A Clojure web applications library"
     :copyright "Copyright 2009-2010 Mark McGranaghan"
     :root "."
     :source-path ""
     :web-src-dir "http://github.com/mmcgrana/ring/blob/"
     :web-home "http://mmcgrana.github.com/ring/"
     :output-path "autodoc"
     :namespaces-to-document ["ring"]
     :trim-prefix "ring."
     :load-except-list [#"/example/" #"/test/" #"project\.clj"]})

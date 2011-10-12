(defproject ring "1.0.0-beta2"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies
    [[ring/ring-core "1.0.0-beta2"]
     [ring/ring-devel "1.0.0-beta2"]
     [ring/ring-jetty-adapter "1.0.0-beta2"]
     [ring/ring-servlet "1.0.0-beta2"]]
  :dev-dependencies
    [[org.clojars.weavejester/autodoc "0.9.0"]]
  :autodoc
    {:name "Ring"
     :description "A Clojure web applications library"
     :copyright "Copyright 2009-2011 Mark McGranaghan"
     :root "."
     :source-path ""
     :web-src-dir "http://github.com/mmcgrana/ring/blob/"
     :web-home "http://mmcgrana.github.com/ring/"
     :output-path "autodoc"
     :namespaces-to-document ["ring"]
     :load-except-list [#"/example/" #"/test/" #"project\.clj"]})

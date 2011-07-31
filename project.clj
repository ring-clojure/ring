(defproject ring "0.3.11"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies
    [[ring/ring-core "0.3.11"]
     [ring/ring-devel "0.3.11"]
     [ring/ring-jetty-adapter "0.3.11"]
     [ring/ring-servlet "0.3.11"]]
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

(defproject ring/ring-core "1.2.0-SNAPSHOT"
  :description "Ring core libraries."
  :url "https://github.com/ring-clojure/ring"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [commons-codec "1.6"]
                 [commons-io "2.1"]
                 [commons-fileupload "1.2.1"]
                 [javax.servlet/servlet-api "2.5"]
                 [clj-time "0.3.7"]]
  :profiles
  {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.0-RC1"]]}})

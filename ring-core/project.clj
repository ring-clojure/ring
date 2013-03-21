(defproject ring/ring-core "1.2.0-beta1"
  :description "Ring core libraries."
  :url "https://github.com/ring-clojure/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring/ring-codec "1.0.0"]
                 [commons-io "2.4"]
                 [commons-fileupload "1.2.2"]
                 [javax.servlet/servlet-api "2.5"]
                 [clj-time "0.4.4"]]
  :profiles
  {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}})

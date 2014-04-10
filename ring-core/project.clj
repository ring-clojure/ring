(defproject ring/ring-core "1.2.2"
  :description "Ring core libraries."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.reader "0.8.1"]
                 [ring/ring-codec "1.0.0"]
                 [commons-io "2.4"]
                 [commons-fileupload "1.3"]
                 [clj-time "0.6.0"]
                 [crypto-random "1.2.0"]
                 [crypto-equality "1.0.0"]]
  :profiles
  {:provided {:dependencies [[javax.servlet/servlet-api "2.5"]]}
   :dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})

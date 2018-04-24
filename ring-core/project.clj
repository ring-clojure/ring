(defproject ring/ring-core "1.6.3"
  :description "Ring core libraries."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-codec "1.1.1"]
                 [commons-io "2.5"]
                 [commons-fileupload "1.3.3"]
                 [clj-time "0.11.0"]
                 [crypto-random "1.2.0"]
                 [crypto-equality "1.0.0"]]
  :aliases {"test-all" ["with-profile" "default:+1.6:+1.7:+1.8" "test"]}
  :profiles
  {:provided {:dependencies [[javax.servlet/servlet-api "2.5"]]}
   :dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})

(defproject ring/ring-core "1.11.0-alpha1"
  :description "Ring core libraries."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-codec "1.2.0"]
                 [commons-io "2.13.0"]
                 [org.apache.commons/commons-fileupload2-core "2.0.0-M1"]
                 [crypto-random "1.2.1"]
                 [crypto-equality "1.0.1"]]
  :aliases {"test-all" ["with-profile" "default:+1.8:+1.9:+1.10:+1.11" "test"]}
  :profiles
  {:dev  {:dependencies [[clj-time "0.15.2"]]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
   :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}})

#!/usr/bin/env bb

(require '[clojure.string :as str])
(import 'java.time.format.DateTimeFormatter
        'java.time.LocalDateTime)

(def project-files
  ["project.clj"
   "ring-bench/project.clj"
   "ring-core/project.clj"
   "ring-core-protocols/project.clj"
   "ring-devel/project.clj"
   "ring-jakarta-servlet/project.clj"
   "ring-jetty-adapter/project.clj"
   "ring-servlet/project.clj"
   "ring-websocket-protocols/project.clj"])

(def ring-dep-patterns
  ["ring/ring-core"
   "org.ring-clojure/ring-core-protocols"
   "ring/ring-devel"
   "org.ring-clojure/ring-jakarta-servlet"
   "ring/ring-jetty-adapter"
   "ring/ring-servlet"
   "org.ring-clojure/ring-websocket-protocols"])

(def version (first *command-line-args*))

(when-not version
  (println "Error: requires version as first argument.")
  (System/exit 1))

(defn- update-deps [s]
  (reduce (fn [s pattern]
            (let [re (->> (str/re-quote-replacement pattern)
                          (format "\\[%s \"(.*?)\"\\]")
                          (re-pattern))]
              (str/replace s re (format "[%s \"%s\"]" pattern version))))
          s
          ring-dep-patterns))

(doseq [f project-files]
  (-> (slurp f)
      (str/replace #"\(defproject (.*?) \"(.*?)\""
                   (format "(defproject $1 \"%s\"" version))
      (update-deps)
      (as-> s (spit f s)))
  (println (format "Updated '%s'." f)))

(-> (slurp "README.md")
    (str/replace
     #"ring/ring-core \{:mvn/version \"(.*?)\"\}"
     (format "ring/ring-core {:mvn/version \"%s\"}" version))
    (str/replace
     #"\[ring/ring-core (.*?)\]"
     (format "[ring/ring-core \"%s\"]" version))
    (as-> s (spit "README.md" s)))
(println "Updated 'README.md'.")

(def now (LocalDateTime/now))
(def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(->> (slurp "CHANGELOG.md")
     (str "## " version " (" (.format now formatter) ")\n\n"
          "* TBD\n\n")
     (spit "CHANGELOG.md"))

(println "Updated 'CHANGELOG.md'.")
(newline)
(println "Remember to update the CHANGELOG!")

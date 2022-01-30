;(defproject cli4clj "1.7.10"
(defproject cli4clj "1.7.11-SNAPSHOT"
  :description "Create simple interactive CLIs for Clojure applications."
  :url "https://github.com/ruedigergad/cli4clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
; Revert to Clojure 1.9.0 until the following is fixed:
; https://dev.clojure.org/jira/browse/CLJ-1472
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [clj-assorted-utils "1.18.8"]
                 [org.clojure/core.async "1.5.648"]
                 [org.jline/jline "3.21.0"]
                 [org.fusesource.jansi/jansi "2.4.0"]
                 ]
  :global-vars {*warn-on-reflection* true}
  :html5-docs-docs-dir "docs/doc"
  :html5-docs-ns-includes #"^cli4clj.*"
  :html5-docs-repository-url "https://github.com/ruedigergad/cli4clj/blob/master"
  :test2junit-output-dir "docs/test-results"
  :test2junit-run-ant true
  :main cli4clj.example
  :aot :all
  :plugins [[lein-cloverage "1.0.9"] [test2junit "1.4.2"] [lein-html5-docs "3.0.3"]]
  :profiles  {:repl  {:dependencies  [[jonase/eastwood "1.2.2" :exclusions  [org.clojure/clojure]]]}
              :run {:jvm-opts ["-Djava.util.logging.config.file=logging.properties"]}
              }
  :jvm-opts ["-Djava.util.logging.config.file=logging.properties"]
)

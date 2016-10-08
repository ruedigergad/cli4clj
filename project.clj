;(defproject cli4clj "1.2.3"
(defproject cli4clj "1.2.4-SNAPSHOT"
  :description "Create simple interactive CLIs for Clojure applications."
  :url "https://github.com/ruedigergad/cli4clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-assorted-utils "1.14.0"]
                 [jline/jline "2.14.2"]]
  :global-vars {*warn-on-reflection* true}
  :html5-docs-docs-dir "ghpages/doc"
  :html5-docs-ns-includes #"^cli4clj.*"
  :html5-docs-repository-url "https://github.com/ruedigergad/cli4clj/blob/master"
  :test2junit-output-dir "ghpages/test-results"
  :test2junit-run-ant true
  :main cli4clj.example
  :plugins [[lein-cloverage "1.0.2"] [test2junit "1.2.4"] [lein-html5-docs "3.0.3"]]
  :profiles  {:repl  {:dependencies  [[jonase/eastwood "0.2.2" :exclusions  [org.clojure/clojure]]]}}
)

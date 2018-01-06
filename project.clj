;(defproject cli4clj "1.3.2"
(defproject cli4clj "1.3.3-SNAPSHOT"
  :description "Create simple interactive CLIs for Clojure applications."
  :url "https://github.com/ruedigergad/cli4clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-assorted-utils "1.18.2"]
                 [org.clojure/core.async "0.3.443"]
                 [jline/jline "2.14.2"]]
  :global-vars {*warn-on-reflection* true}
  :html5-docs-docs-dir "ghpages/doc"
  :html5-docs-ns-includes #"^cli4clj.*"
  :html5-docs-repository-url "https://github.com/ruedigergad/cli4clj/blob/master"
  :test2junit-output-dir "ghpages/test-results"
  :test2junit-run-ant true
  :main cli4clj.example
  :plugins [[lein-cloverage "1.0.2"] [test2junit "1.3.3"] [lein-html5-docs "3.0.3"]]
  :profiles  {:repl  {:dependencies  [[jonase/eastwood "0.2.4" :exclusions  [org.clojure/clojure]]]}}
)

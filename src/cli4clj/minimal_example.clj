(ns cli4clj.minimal-example (:gen-class)
  (:require (cli4clj [cli :as cli])))
(defn divide [x y] (/ x y)) ;;; Used for example below.

(defn -main [& args]
  (cli/start-cli
    {:cmds
      {:test-cmd {:fn #(println "This is a test.")
                  :short-info "Test Command"
                  :long-info "Prints a test message."}
       :add {:fn (fn [summand1 summand2] (+ summand1 summand2))
             :completion-hint "Enter two values to add."}
       :divide {:fn divide}}
:allow-eval true, :alternate-scrolling (some #(= % "alt") args)}))

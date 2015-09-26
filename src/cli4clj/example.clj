;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "This is a simple example for using cli4clj.
          The example can be run via \"lein run\"."}    
  cli4clj.example
  (:use cli4clj.cli)
  (:gen-class))

;;; This function is just used for providing an example below.
(defn divide [x y]
  (/ x y))

(defn -main [& args]
;;; In the simplest scenario only the commands have to be defined.
;;; In the following a simple scenario with a few explanations is provided:
;;;               Commands are stored in a nested map using the key :cmds in the options map.
;;;               !      As simple example command, we define a command called "test".
;;;               !      !      The function to be called for the command is defined via :fn.
;;;               !      !      !  In this example we use an anonymous function.
;;;               !      !      !  However, arbitrary functions can be used.
  (let [cli-opts {:cmds {:test {:fn #(println "This is a test.")
;;;                             Optionally a short information text can be given.
                                :short-info "Test Command"
;;;                             Another long information text can be optionally given as well.
                                :long-info "Prints a test message to stdout."}
;;;                      "Aliases" can be used, e.g., for defining shortcuts by relating to existing commands.
;;;                      The order in which aliases and commands are defined does not matter.
;;;                      Just make sure that the alias refers to an existing command.
                         :t :test
;;;                      Commands can also accept arguments as shown for the "add" example."
;;;                      All Clojure data types are supported as arguments.
;;;                      However, no sanity checks, e.g., with respect to the number of arguments or the argument type(s), are performed.
;;;                      If things go wrong, exceptions will be thrown and printed.
                         :add  {:fn (fn [a b] (+ a b))
                                :short-info "Add two values."}
                         :a :add
;;;                      The following example shows the use of a named function.
                         :divide {:fn divide
                                  :short-info "Divide two values."
                                  :long-info "The first argument will be divided by the second argument."}
                         :d :divide
;;;                      "h" is already a pre-defined command but it can be overridden.
;;;                      This is shown by using the definition of an alias for test as example.
                         :h :test
;;;                      The pre-defined "help" command, however, cannot be overridden.
;;;                      Please note that "help" and "exit" cannot be overridden by any user defined command.
                         :help :test
                        }
                 }]
;;;  Last but not least, the CLI is started using the options defined above.
;;;  Please note that this call will block as long as the CLI is executed.
    (start-cli cli-opts)))


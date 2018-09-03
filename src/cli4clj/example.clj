;;;
;;;   Copyright 2015-2018 Ruediger Gad
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
  (:require
    (cli4clj [cli :as cli])
    (clj-assorted-utils [util :as utils])
    (clojure [pprint :as pprint]))
  (:gen-class))

;;; This function is just used for providing an example below.
(defn divide [numer denom]
  (/ numer denom))

(defn -main [& args]
;;; Simple cli4clj usage example:
;;; In the simplest scenario only the commands have to be defined.
;;; In the following, an example using a simple scenario along with some explanations is provided:
;;;              cli4clj is configured via an "options" map.
;;;              | Commands are stored in a nested map using the key :cmds in the options map.
;;;              |  |      As first simple example command, we define a command called "test-cmd".
;;;              |  |      |         The function to be called for the command is defined via :fn.
;;;              |  |      |         |  In this example we use an anonymous function.
;;;              |  |      |         |  However, arbitrary functions can be used.
  (cli/start-cli {:cmds {:test-cmd {:fn #(println "This is a test.")
;;;                                 Optionally a short information text can be given.
                                    :short-info "Test Command"
;;;                                 Another long information text can be optionally given as well.
                                    :long-info "Prints a test message to stdout."}
;;;                      "Aliases" can be used, e.g., for defining shortcuts by relating to existing commands.
;;;                      The order in which aliases and commands are defined does not matter.
;;;                      Just make sure that the alias refers to an existing command.
                         :t :test-cmd
;;;                      Commands can also accept arguments as shown for the "add" example."
;;;                      All Clojure data types are supported as arguments.
;;;                      However, no sanity checks, e.g., with respect to the number of arguments or the argument type(s), are performed.
;;;                      If things go wrong, exceptions will be thrown and printed.
                         :add  {:fn (fn [summand1 summand2] (+ summand1 summand2))
                                :short-info "Add two values."}
                                :a :add
;;;                      cli4clj already provides some pre-defined commands, from which some can overridden while others cannot.
;;;                      "h" is a pre-defined command but it can be overridden.
;;;                      This is shown by using the definition of an alias to test as example.
                         :h :test
;;;                      The pre-defined "help" command, however, cannot be overridden.
;;;                      Please note that "help" and "exit" cannot be overridden by any user defined command.
                         :help :test
;;;                      The following simple example command is used to illustrate that arbitrary Clojure data types can be used.
;;;                      The command is intended to take a seq (e.g., a list or vector) which it converts into a CSV string.
                         :to-csv {:fn (fn [data] (reduce (fn [s d] (str s "," d)) (str (first data)) (rest data)))
;;;                               A "completion-hint" can be displayed along with the function arguments via tab-completion.
;;;                               In the CLI, press the tab-key to test the tab-completion feature.
                                  :completion-hint "The data argument can be of any Clojure sequence type, e.g., [1 2 3] or (:a :b :c). Note that the list is not quoted."
                                  :short-info "Seq to CSV"
                                  :long-info "E.g.: \"to-csv [1 2 3]\""}
;;;                      The following example shows the use of a named function.
;;;                      The divide function is also used to illustrate the behavior on errors during processing (e.g., try to divide by 0).
                         :divide {:fn divide
;;;                               The completion-hint may also refer to another command property such as the short info.
                                  :completion-hint :short-info
                                  :short-info "Divide two values."
                                  :long-info "The first argument will be divided by the second argument."}
                         :d :divide
;;;                      The following example shows the use of optional arguments.
;;;                      It can also be used to test how different types of inputs are treated.
                         :print-cmd {:fn (fn [arg & opt-args]
                                           (print "Arg-type:" (type arg) "Arg: ")
                                           (pprint/pprint arg)
                                           (print "Opt-args: ")
                                           (pprint/pprint opt-args))
                                     :short-info "Pretty print the supplied arguments."
                                     :long-info "This function pretty prints its supplied arguments. It takes at least one argument."}
                         :p :print
                         :print-repeat {:fn (fn [text interval]
                                              (utils/run-repeat (utils/executor) #(println text) interval))
                                        :short-info "Repeatedly print text with the given interval in milliseconds."}}
                  :allow-eval true
                  :prompt-string "cli# "
                  :alternate-scrolling (some #(= % "alt") args)
                  :alternate-height 3}))

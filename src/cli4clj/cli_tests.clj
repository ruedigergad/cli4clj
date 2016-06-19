;;;
;;;   Copyright 2015, 2016 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Helper functions for testing CLIs."}
  cli4clj.cli-tests
  (:use
    [clojure.main :only [repl skip-if-eol skip-whitespace]]
    [clojure.string :only [blank? split]])
  (:require
    (clj-assorted-utils [util :refer :all])
    (cli4clj [cli :refer :all])))

(defn cmd-vector-to-test-input-string
  "This function takes a vector of string commands and creates a one-line command that is suited to being passed to a cli instance during testing."
  [cmd-vec]
  (reduce (fn [s c] (str s c *cli4clj-line-sep*)) "" cmd-vec))

(defn create-repl-read-test-fn
  "This function creates a repl read function for testing."
  [cmds prompt-string]
  (create-repl-read-fn cmds))

(defn exec-tested-fn
  "This function takes another function as argument and executes it in a way that is suited for testing."
  [tested-fn]
  (binding [*read-factory* create-repl-read-test-fn]
    (tested-fn)))

(defn test-cli-stdout
  "Takes a function to be tested and a vector of string input commands and returns the string that was printed to stdout as a result of executing the supplied commands in the cli provided by the tested-fn."
  [tested-fn in-cmds]
  (.trim (with-out-str (with-in-str (cmd-vector-to-test-input-string in-cmds) (exec-tested-fn tested-fn)))))

(defn test-cli-stderr
  "Takes a function to be tested and a vector of string input commands and returns the string that was printed to stderr as a result of executing the supplied commands in the cli provided by the tested-fn."
  [tested-fn in-cmds]
  (.trim (with-err-str (with-out-str (with-in-str (cmd-vector-to-test-input-string in-cmds) (exec-tested-fn tested-fn))))))

(defn expected-string
  "Takes a vector of strings that are intended to represent individual line of expected command line output and converts them into a string that can be compared against the output of the test-cli-stdout and test-cli-stderr functions.
   The most notably property is that the lines are joined based on the platform dependent line.separator."
  ([expected-lines]
    (expected-string expected-lines *cli4clj-line-sep*))
  ([expected-lines separator]
    (reduce
      (fn [s e] (str s separator e))
      (first expected-lines)
      (rest expected-lines))))


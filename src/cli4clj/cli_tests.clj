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
  (:require
    (clj-assorted-utils [util :as utils])
    (cli4clj [cli :as cli])))

(defn cmd-vector-to-test-input-string
  "This function takes a vector of string commands and creates a one-line command that is suited to being passed to a cli instance during testing."
  [cmd-vec]
  (reduce (fn [s c] (str s c cli/*cli4clj-line-sep*)) "" cmd-vec))

(defn create-repl-read-test-fn
  "This function creates a repl read function for testing."
  [opts]
  (cli/create-repl-read-fn opts))

(defn exec-tested-fn
  "This function takes another function as argument and executes it in a way that is suited for testing."
  [tested-fn]
  (binding [cli/*read-factory* create-repl-read-test-fn]
    (tested-fn)))

(defn test-cli-stdout-cb
  "Takes a function to be tested and a vector of string input commands and returns the string that was printed to stdout as a result of executing the supplied commands in the cli provided by the tested-fn.
   In addition the function cb-fn will be called for each element that is written to stdout."
  [tested-fn in-cmds cb-fn]
  (.trim (utils/with-out-str-cb
           cb-fn
           (with-in-str (cmd-vector-to-test-input-string in-cmds)
             (exec-tested-fn tested-fn)))))

(defn test-cli-stdout
  "Takes a function to be tested and a vector of string input commands and returns the string that was printed to stdout as a result of executing the supplied commands in the cli provided by the tested-fn."
  ([tested-fn in-cmds]
    (.trim (with-out-str (with-in-str (cmd-vector-to-test-input-string in-cmds) (exec-tested-fn tested-fn)))))
  ([tested-fn in-cmds sl]
    (test-cli-stdout-cb
      (fn []
        (tested-fn)
        (sl :await-completed))
      in-cmds
      (fn [s]
        (sl s)))))

(defn test-cli-stderr-cb
  "Takes a function to be tested and a vector of string input commands and returns the string that was printed to stdout as a result of executing the supplied commands in the cli provided by the tested-fn.
   In addition the function cb-fn will be called for each element that is written to stderr."
  [tested-fn in-cmds cb-fn]
  (.trim (utils/with-err-str-cb
           cb-fn
           (with-in-str (cmd-vector-to-test-input-string in-cmds)
             (exec-tested-fn tested-fn)))))

(defn test-cli-stderr
  "Takes a function to be tested and a vector of string input commands and returns the string that was printed to stderr as a result of executing the supplied commands in the cli provided by the tested-fn."
  ([tested-fn in-cmds]
    (.trim (utils/with-err-str (with-in-str (cmd-vector-to-test-input-string in-cmds) (exec-tested-fn tested-fn)))))
  ([tested-fn in-cmds sl]
    (test-cli-stderr-cb
      (fn []
        (tested-fn)
        (sl :await-completed))
      in-cmds
      (fn [s]
        (sl s)))))

(defn expected-string
  "Takes a vector of strings that are intended to represent individual lines of expected command line output and converts them into a string that can be compared against the output of the test-cli-stdout and test-cli-stderr functions.
   The most notably property is that the lines are joined based on the platform dependent line.separator."
  ([expected-lines]
    (expected-string expected-lines cli/*cli4clj-line-sep*))
  ([expected-lines separator]
    (reduce
      (fn [s e] (str s separator e))
      (first expected-lines)
      (rest expected-lines))))

(defn string-latch
  [sl-defs]
  (let [flags (doall
                (mapv
                  (fn [sl-def]
                    (if (string? sl-def)
                      [sl-def (utils/prepare-flag) nil]
                      [(first sl-def) (utils/prepare-flag) (second sl-def)]))
                  sl-defs))
        completed-flag (utils/prepare-flag)
        observed-values (atom [])
        set-index (atom 0)
        await-index (atom 0)]
    (fn
      ([]
        (utils/await-flag (get-in flags [@await-index 1]))
        (swap! await-index inc)
        @observed-values)
      ([s]
        (condp = s
          :await-completed (utils/await-flag completed-flag)
          (do
            (swap! observed-values conj s)
            (when (= s (get-in flags [@set-index 0]))
              (when-let [f (get-in flags [@set-index 2])]
                (f @observed-values))
              (utils/set-flag (get-in flags [@set-index 1]))
              (swap! set-index inc)
              (when (= @set-index (count flags))
                (utils/set-flag completed-flag)))))))))


;;;
;;;   Copyright 2015, 2016 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "cli4clj allows to create simple interactive command line interfaces for Clojure applications.
          For an example usage scenario please see the namespace cli4clj.example."}
  cli4clj.cli
  (:use
    [clojure.main :only [repl skip-if-eol skip-whitespace]]
    [clojure.string :only [blank? split]])
  (:require
    (clj-assorted-utils [util :refer :all])
    (clojure [stacktrace :refer :all]))
  (:import
    (java.io PushbackReader StringReader)
    (jline.console ConsoleReader)
    (jline.console.completer ArgumentCompleter Completer StringsCompleter)))

(def ^:dynamic *comment-begin-string* ";")

(def ^:dynamic *jline-input-stream* System/in)
(def ^:dynamic *jline-output-stream* System/out)

(def ^:dynamic *cli4clj-line-sep* (System/getProperty "line.separator"))

(defn cli-repl-print
  "The default repl print function of cli4clj only prints non-nil values."
  [arg]
  (if (not (nil? arg))
    (prn arg)))

(defn get-cmd-aliases
  "This function is used to find all alias definitions for the respective full command definitions.
   It takes a map of commands as defined in the cli4clj configuration as argument.
   The returned map maps the keys of the full command definitions to vectors of their corresponding aliases."
  [cmds]
  (reduce
    (fn [m e]
      (let [k (key e)
            v (val e)]
      (if (keyword? v)
        (let [aliases (m v)]
          (assoc m
                 v
                 (if (vector? aliases)
                   (vec (sort (conj aliases k)))
                   [k])))
        m)))
    {}
    cmds))

(defn create-repl-read-fn
  "This function creates a function that is intended to be used as repl read function.
   The created read function is largely based on the existing repl read function:
   http://clojure.github.io/clojure/clojure.main-api.html#clojure.main/repl-read
   The main difference is that if the first argument on a line is a keyword,
   all elements on that line will be forwarded in a vector instead of being
   forwarded seperately."
  [opts]
  (let [cmds (opts :cmds)
        err-fn (opts :print-err)
        invalid-token-to-string (opts :invalid-token-to-string)
        quit-commands (conj (:quit (get-cmd-aliases cmds)) :quit)]
    (fn [request-prompt request-exit]
      (try
        (or ({:line-start request-prompt :stream-end request-exit}
             (skip-whitespace *in*))
            (loop [v []]
              (let [input (try
                            (read {:read-cond :allow} *in*)
                            (catch RuntimeException e
                              (let [msg (.getMessage e)
                                    invalid-msg-start "Invalid token: "]
                                (if (and
                                      invalid-token-to-string
                                      (.contains msg invalid-msg-start))
                                  (.replaceAll msg (str ".*" invalid-msg-start) "")
                                  (throw e)))))]
                (if (and (not (symbol? input)) (empty? v))
                  (do
                    (skip-if-eol *in*)
                    input)
                  (if (= :line-start (skip-whitespace *in*))
                    (if (and (symbol? input)
                             (some #(= (keyword input) %) quit-commands))
                      request-exit
                      (conj v input))
                    (recur (conj v input)))))))
        (catch Exception e
          (err-fn e opts))))))

(defn create-arg-hint-completers
  "This function creates a vector of jline2 ArgumentCompleter instances for displaying hints related to commands via tab-completion."
  [cmds]
  (let [cmd-aliases (get-cmd-aliases cmds)]
    (reduce
      (fn [v k]
        (let [fn-args (get-in cmds [k :fn-args])
              completion-val (get-in cmds [k :completion-hint])
              completion-hint (if (keyword? completion-val)
                                (get-in cmds [k completion-val])
                                completion-val)]
          (if (or
                (not (nil? fn-args))
                (not (nil? completion-hint)))
            (conj
              v
              (ArgumentCompleter.
                [(StringsCompleter. (conj (mapv name (cmd-aliases k)) (name k)))
                 (proxy [Completer] []
                   (complete [buffer cursor candidates]
                     (if (not (nil? fn-args))
                       (.add candidates (str "Arguments: " fn-args)))
                     (if (not (nil? completion-hint))
                       (.add candidates (str completion-hint)))
                     (if (not (and (not (nil? fn-args)) (not (nil? completion-hint))))
                       (.add candidates ""))
                     0))]))
            v)))
      []
      (keys cmds))))

(defn create-jline-read-fn
  "This function creates a read function that leverages jline2 for handling input.
   Thanks to the functionality provided by jline2, this allows, e.g., command history, command editing, or tab-completion.
   The input that is read is then forwarded to a repl read function that was created with create-repl-read-fn."
  [opts]
  (let [cmds (opts :cmds)
        err-fn (opts :print-err)
        prompt-string (opts :prompt-string)
        in-rdr (doto (ConsoleReader. nil *jline-input-stream* *jline-output-stream* nil)
                 (.addCompleter (StringsCompleter. (remove #(.startsWith % "_") (map name (keys cmds)))))
                 (.setPrompt prompt-string))
        arg-hint-completers (create-arg-hint-completers cmds)
        _ (doseq [compl arg-hint-completers]
            (.addCompleter in-rdr compl))
        rdr-fn (create-repl-read-fn opts)]
    (fn [request-prompt request-exit]
      (let [line (.readLine in-rdr)]
        (if (and (not (nil? line))
                 (not (.isEmpty line))
                 (not (-> line (.trim) (.startsWith *comment-begin-string*))))
          (binding [*in* (PushbackReader. (StringReader. (str line *cli4clj-line-sep*)))]
            (rdr-fn request-prompt request-exit))
          request-prompt)))))

(defn resolve-cmd-alias
  "This function is used to resolve the full command definition for a given command alias.
   If a full command definition is passed as input-cmd, the input-cmd will be returned as-is.
   If an alias is passed as input-cmd, the full command definition will be looked up in cmds and returned."
  [input-cmd cmds]
  (if (keyword? (cmds input-cmd))
    (cmds input-cmd)
    input-cmd))

(defn create-cli-eval-fn
  "This function creates the default eval function as used by cli4clj.
   When allow-eval is false, only commands defined in cmds will be allowed to be executed.
   In case of exceptions, print-err will be called with the respective exception as argument."
  [opts]
  (let [cmds (:cmds opts)
        allow-eval (:allow-eval opts)
        err-fn (:print-err opts)]
    (fn [arg]
      (try
        (if (and (vector? arg) (contains? cmds (keyword (first arg))))
          (let [cmd (resolve-cmd-alias (keyword (first arg)) cmds)]
            (apply
              (get-in cmds [cmd :fn])
              (rest arg)))
          (if allow-eval
            (eval arg)
            (err-fn (str "Invalid command: \"" arg "\". Please type \"help\" to get an overview of commands.") opts)))
        (catch Exception e
          (err-fn e opts))))))

(defn create-cli-help-fn
  "This function is used to create the default help function.
   The help output is created based on the cli4clj configuration that is passed via the options argument."
  [options]
  (let [cmds (:cmds options)
        cmd-aliases (get-cmd-aliases cmds)
        command-names (sort (keys cmds))
        cmd-entry-delimiter (:help-cmd-entry-delimiter options)]
    (fn []
      (doseq [c command-names]
        (when (and
                (map? (cmds c))
                (not (.startsWith (name c) "_")))
          (print (name c))
          (when-let [al (c cmd-aliases)]
            (print "" (vec (map #(symbol (name %)) al))))
          (println "")
          (when-let [si (get-in cmds [c :short-info])]
            (print (str "\t" si)))
          (when-let [args (get-in cmds [c :fn-args])]
            (print "\t" "Arguments:" args))
          (println "")
          (when-let [li (get-in cmds [c :long-info])]
            (println (str "\t" li)))
          (print cmd-entry-delimiter))))))

(defn get-cli-mandatory-default-options
  "Create a map with the mandatory default options.
   The mandatory default options cannot be overridden by the user."
  []
  (let [print-exception-trace (atom false)]
    {:cmds {:quit {:fn (fn [] (println "Error: The quit function should never be called."))
                   :short-info "Quit the CLI."
                   :long-info "Terminate and close the command line interface."}
            :help {:short-info "Show help."
                   :long-info "Display a help text that lists all available commands including further detailed information about these commands."}
            :_enable-trace {:fn (fn [arg]
                                  (if (instance? java.lang.Boolean arg)
                                    (reset! print-exception-trace arg)
                                    (println "Error, you need to supply a boolean value: true or false"))
                                  (println "print-exception-trace is set to:" @print-exception-trace))
                            :short-info "Enable/Disable Printing of Full Exception Traces"
                            :long-info "When set to false (default), only the exception message will be printed when an exception occurs. When set to true, the full traces of exceptions will be printed."}
            :_sleep {:fn (fn [duration] (sleep duration))
                     :short-info "Sleep for n milliseconds."
                     :long-info "Pause the UI thread for n milliseconds. One use case for this is unit testing of CLIs with asynchronous interaction."}}
     :print-exception-trace (fn [] @print-exception-trace)}))

(defn print-err-fn
  "This is the default function for printing error messages.
   If the supplied argument is an exception, the exception message will be printed to stderr.
   Otherwise, the string representation of the passed argument is printed to stderr."
  [arg opts]
  (cond
    (and
      (instance? Exception arg)
      ((:print-exception-trace opts))) (print-cause-trace arg)
    (instance? Exception arg) (println-err (.getMessage arg))
    :default (println-err (str arg))))

(def cli-default-options
  {:allow-eval false
   :cmds {:h :help
          :? :help
          :q :quit}
   :eval-factory create-cli-eval-fn
   :help-factory create-cli-help-fn
   :help-cmd-entry-delimiter *cli4clj-line-sep*
   :invalid-token-to-string true
   :print cli-repl-print
   :print-err print-err-fn
   :prompt-fn (fn [])
   :prompt-string "cli# "})

(defn merge-options
  "This function merges the user supplied configuration options with the default and mandatory default options.
   For creating the actual cli4clj configuration, please use get-cli-opts as get-cli-opts will, e.g., also create and inject the help function."
  [defaults user-options mandatory-defaults]
  (merge-with
    (fn [a b] (if (and (map? a) (map? b))
                (merge a b)
                b))
    defaults user-options mandatory-defaults))

(defn get-cli-opts
  "This function creates the actual cli4clj configuration based on the supplied user configuration."
  [user-options]
  (let [merged-opts (merge-options cli-default-options user-options (get-cli-mandatory-default-options))
        help-fn ((merged-opts :help-factory) merged-opts)]
    (assoc-in merged-opts [:cmds :help :fn] help-fn)))

(def ^:dynamic *read-factory* create-jline-read-fn)

(defn add-args-info
  "This function adds information about the arguments of the commands into the configuration.
   The default behavior can be overridden by setting :fn-args for commands."
  [opts]
  (reduce
    (fn [m k]
      (let [f (get-in opts [:cmds k :fn])
            args (cond
                   (symbol? f) (map-quote-vec (get-defn-arglists (eval `(var ~f))))
                   (and (list? f)
                        (= 'fn (first f))) (:args (get-fn-arglists f))
                   :default nil)]
        (if (and
              (not (nil? args))
              (nil? (get-in m [:cmds k :fn-args])))
          (assoc-in m [:cmds k :fn-args] args)
          m)))
    opts
    (-> opts :cmds keys)))

(defmacro add-args-info-m
  "Macro version of add-args-info.
   This is primarily used for testing and directly forwards the evaluation to add-args-info."
  [opts]
  (add-args-info opts))

(defmacro start-cli
  "This is the primary entry point for starting and configuring cli4clj.
   Please note that the configuration options can also be defined in a global or local var.
   However, in order to lookup arguments defined in anonymous functions, the configuration options have to be defined directly in the macro call."
  [user-options]
   (let [options-with-args-info (add-args-info user-options)]
    `(let [options# (get-cli-opts ~options-with-args-info)]
       (repl
         :eval ((options# :eval-factory) options#)
         :print (options# :print)
         :prompt (options# :prompt-fn)
         :read (*read-factory* options#)))))


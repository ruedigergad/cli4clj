;;;
;;;   Copyright 2015-2021 Ruediger Gad
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
  (:require
    (clj-assorted-utils [util :as utils])
    (clojure
      [main :as main]
      [stacktrace :as strace])
    (clojure.java [io :as jio])
    (clojure.core [async :as async]))
  (:import
    (java.io PushbackReader StringReader)
    (jline TerminalFactory TerminalFactory$Flavor UnsupportedTerminal)
    (jline.console ConsoleReader)
    (jline.console.completer ArgumentCompleter Completer StringsCompleter)
    (jline.console.history FileHistory)))

(def ^:dynamic *comment-begin-string* ";")

(def ^:dynamic *jline-input-stream* System/in)
(def ^:dynamic *jline-output-stream* System/out)

(def ^:dynamic *line-sep* (System/getProperty "line.separator"))

(defn cli-repl-print
  "The default repl print function of cli4clj only prints non-nil values."
  [arg]
  (when (not (nil? arg))
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

(defn init
  "Internal init function for setting up the environment."
  [options]
  (let [cli-fns-ns 'cli4clj-cli-fns]
    (create-ns cli-fns-ns)
    (doseq [[cmd-name cmd-def] (options :cmds)]
      (when (map? cmd-def)
        (let [cmd-sym (symbol (name cmd-name))
              cmd-res (resolve cmd-sym)]
          (when (or
                (nil? cmd-res)
                (= (name cli-fns-ns) (str (:ns (meta cmd-res)))))
            (intern cli-fns-ns (symbol (name cmd-name)) (:fn cmd-def))))))
    (refer cli-fns-ns)))

(defn skip-whitespace
;;; The code for this function was largely taken from
;;; https://github.com/clojure/clojure/blob/b182982007df934394f0bc68b3a238ca9f200dd1/src/clj/clojure/main.clj#L120
;;; which is copyrighted as:
;;; ;; Copyright (c) Rich Hickey All rights reserved. The use and
;;; ;; distribution terms for this software are covered by the Eclipse Public
;;; ;; License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can be found
;;; ;; in the file epl-v10.html at the root of this distribution. By using this
;;; ;; software in any fashion, you are agreeing to be bound by the terms of
;;; ;; this license. You must not remove this notice, or any other, from this
;;; ;; software.
;;; ;; Originally contributed by Stephen C. Gilardi
;;;
;;; Changes aim at "making GraalVM happy" and include::
;;; - Add type hints.
;;; - Remove "readLine" case.
  [#^PushbackReader s]
  (loop [c (.read s)]
	(cond
	  (= c (int \newline)) :line-start
	  (= c -1) :stream-end
	  (or (Character/isWhitespace (char c)) (= c (int \,))) (recur (.read s))
	  :else (do (.unread s c) :body))))

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
        quit-commands (conj (:quit (get-cmd-aliases cmds)) :quit)
        read-fn (opts :read-fn)]
    (init opts)
    (fn [request-prompt request-exit]
      (try
        (or ({:line-start request-prompt :stream-end request-exit}
              (skip-whitespace *in*))
            (loop [v []]
              (let [input (try
                            (read-fn {:read-cond :allow} *in*)
                            (catch RuntimeException e
                              (let [msg (.getMessage e)]
                                (if (and
                                      invalid-token-to-string
                                      (.contains msg "Invalid"))
                                  (.replaceAll msg ".* Invalid \\w*: " "")
                                  (throw e)))))]
                (if (and (not (symbol? input)) (empty? v))
                  (do
                    (main/skip-if-eol *in*)
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
                #^"[Ljline.console.completer.Completer;"
                (into-array
                  Completer
                  [(StringsCompleter. #^java.util.Collection (conj (mapv name (cmd-aliases k)) (name k)))
                   (proxy [Completer] []
                     (complete [buffer cursor #^java.util.List candidates]
                       (when (not (nil? fn-args))
                         (.add candidates (str "Arguments: " fn-args)))
                       (when (not (nil? completion-hint))
                         (.add candidates (str completion-hint)))
                       (when (not (and (not (nil? fn-args)) (not (nil? completion-hint))))
                         (.add candidates ""))
                       0))])))
            v)))
      []
      (keys cmds))))

(defn set-up-alternate-scrolling
  [height width alternate-height alternate-scroll-separator prompt-string #^ConsoleReader in-rdr & [entry-message]]
  (let [prompt-width (count prompt-string)
        adjusted-prompt-string (str "\u001B[" (- height alternate-height) ";0H"
                                    "\u001B[2K"
                                    prompt-string
                                    "\u001B[" (- height alternate-height 1) ";0H" (apply str (repeat width alternate-scroll-separator))
                                    "\u001B[1;" (- height alternate-height 2) "r"
                                    "\u001B[" (- height alternate-height) ";" (+ prompt-width 1) "H")]
    (print (str "\u001B[2J\u001B[1;" (- height alternate-height 2) "r"))
    (flush)
    (.setPrompt in-rdr adjusted-prompt-string)
    (when entry-message
      (print entry-message))))

(defn create-jline-read-fn
  "This function creates a read function that leverages jline2 for handling input.
   Thanks to the functionality provided by jline2, this allows, e.g., command history, command editing, or tab-completion.
   The input that is read is then forwarded to a repl read function that was created with create-repl-read-fn."
  [opts]
  (let [cmds (opts :cmds)
        entry-message (opts :entry-message)
        prompt-string (opts :prompt-string)
        in-rdr (doto (ConsoleReader. nil *jline-input-stream* *jline-output-stream* nil)
                 (.addCompleter (StringsCompleter.
                                  #^java.util.Collection
                                  (remove
                                    #(.startsWith #^java.lang.String % "_")
                                    (map name (keys cmds)))))
                 (.setPrompt prompt-string))
        file-history (if (opts :persist-history)
                       (let [history-file-name (if (contains? opts :history-file-name)
                                                 (opts :history-file-name)
                                                 (str
                                                   (System/getProperty "user.home")
                                                   "/."
                                                   (opts :calling-ns)
                                                   ".history"))
                             history-file (jio/file history-file-name)]
                         (FileHistory. history-file))
                       nil)
        _ (when (not (nil? file-history))
            (.setHistory in-rdr file-history))
        arg-hint-completers (create-arg-hint-completers cmds)
        _ (doseq [compl arg-hint-completers]
            (.addCompleter in-rdr compl))
        rdr-fn (create-repl-read-fn opts)

        alternate-scrolling (opts :alternate-scrolling)
        alternate-height (opts :alternate-height)
        alternate-scroll-separator (opts :alternate-scroll-separator)
        term (TerminalFactory/create)
        ansi-support (.isAnsiSupported term)
        last-height (atom (.getHeight term))
        last-width (atom (.getWidth term))]
    (if (and alternate-scrolling ansi-support)
      (set-up-alternate-scrolling @last-height @last-width alternate-height alternate-scroll-separator prompt-string in-rdr entry-message)
      (print (str entry-message "\n")))
    (fn [request-prompt request-exit]
      (let [line (.readLine in-rdr)
            current-height (.getHeight term)
            current-width (.getWidth term)]
        (when (and
                alternate-scrolling
                ansi-support)
          (when (or (not= @last-height current-height) (not= @last-width current-width))
            (reset! last-height current-height)
            (reset! last-width current-width)
            (set-up-alternate-scrolling @last-height @last-width alternate-height alternate-scroll-separator prompt-string in-rdr entry-message))
          (print "\u001B[s")
          (print (str "\u001B[" (- current-height alternate-height 3) ";1H> " line))
          (print "\u001B[u")
          (flush))
        (when (not (nil? file-history))
          (.flush file-history))
        (cond
          (and (not (nil? line))
               (not (.isEmpty line))
               (not (-> line (.trim) (.startsWith *comment-begin-string*))))
            (binding [*in* (PushbackReader. (StringReader. (str line *line-sep*)))]
              (rdr-fn request-prompt request-exit))

          (nil? line) request-exit

          :else request-prompt)))))

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
        err-fn (:print-err opts)
        term (TerminalFactory/create)
        alternate-scrolling (:alternate-scrolling opts)
        alternate-height (:alternate-height opts)]
    (fn [arg]
      (when alternate-scrolling
        (-> System/out (.print (str "\u001B[" (- (.getHeight term) alternate-height -1) ";0H" "\u001B[0J"))))
      (try
        (cond
          (and (vector? arg) (contains? cmds (keyword (first arg))))
            (let [cmd (resolve-cmd-alias (keyword (first arg)) cmds)]
              (apply
                (get-in cmds [cmd :fn])
                (rest arg)))
          (and allow-eval (list? arg)) (eval arg)
          :else (err-fn (str "Invalid command: \"" arg "\". Please type \"help\" to get an overview of commands.") opts))
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
          (when-let [args (get-in cmds [c :fn-args])]
            (print " -- Arguments:" args))
          (println "")
          (when-let [si (get-in cmds [c :short-info])]
            (print (str "\t" si)))
          (println "")
          (when-let [li (get-in cmds [c :long-info])]
            (println (str "\t" li)))
          (print cmd-entry-delimiter))))))

(defn get-cli-mandatory-default-options
  "Create a map with the mandatory default options.
   The mandatory default options cannot be overridden by the user."
  []
  (let [print-exception-trace (atom false)]
    {:cmds {:clear {:fn (fn [] (print "\u001B[2J\n"))
                    :short-info "Clear screen."
                    :long-info "Clears the screen and resets the user interface."}
            :help {:short-info "Show help."
                   :long-info "Display a help text that lists all available commands including further detailed information about these commands."}
            :quit {:fn (fn [] (println "Error: The quit function should never be called."))
                   :short-info "Quit the CLI."
                   :long-info "Terminate and close the command line interface."}
            :_enable-trace {:fn (fn [arg]
                                  (if (instance? java.lang.Boolean arg)
                                    (reset! print-exception-trace arg)
                                    (println "Error, you need to supply a boolean value: true or false"))
                                  (println "print-exception-trace is set to:" @print-exception-trace))
                            :short-info "Enable/Disable Printing of Full Exception Traces"
                            :long-info "When set to false (default), only the exception message will be printed when an exception occurs. When set to true, the full traces of exceptions will be printed."}
            :_sleep {:fn (fn [duration] (utils/sleep duration))
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
      ((:print-exception-trace opts))) (strace/print-cause-trace arg)
    (instance? Exception arg) (utils/println-err (.getMessage #^Exception arg))
    :else (utils/println-err (str arg))))

(def cli-default-options
  {:allow-eval false
   :cmds {:h :help
          :? :help
          :q :quit}
   :eval-factory create-cli-eval-fn
   :help-factory create-cli-help-fn
   :help-cmd-entry-delimiter *line-sep*
   :invalid-token-to-string true
   :persist-history true
   :print cli-repl-print
   :print-err print-err-fn
   :prompt-fn (fn [])
   :prompt-string "cli# "
   :read-fn read
   :alternate-scrolling false
   :alternate-height 3
   :alternate-scroll-separator "\u203E"})

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
                   (symbol? f) (utils/map-quote-vec (utils/get-defn-arglists (eval `(var ~f))))
                   (and (list? f)
                        (= 'fn (first f))) (:args (utils/get-fn-arglists f))
                   :else nil)]
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

(defn alt-scroll-writer
  [options]
  (let [alternate-height (options :alternate-height)
        stdout *out*
        new-line (atom true)
        term (TerminalFactory/create)
        x (atom 1)
        y (atom 1)
        wrtr (proxy [java.io.StringWriter] []
                (flush []
                  (reset! new-line true))
                (write
                  ([obj]
                    (let [#^String s (condp instance? obj
                                       java.lang.String obj
                                       java.lang.Integer (str (char obj))
                                       (str obj))
                          term-height (.getHeight term)]
                      (binding [*out* stdout]
                        (when @new-line
                          (reset! x 1)
                          (reset! y (- term-height alternate-height 3)))
                        (print "\u001B[s")
                        (print (str "\u001B[" @y ";" @x "H"))
                        (when @new-line
                          (print "\n"))
                        (print s)
                        (swap! x #(+ % (count s)))
                        (when @new-line
                          (print "\n"))
                        (reset! new-line (.endsWith s "\n"))
                        (print "\u001B[u")
                        (flush))))))]
    wrtr))

(defn get-writer-to-wrap
  [wrtr options]
  (if (@options :alternate-scrolling)
    @wrtr
    *out*))

(defmacro wrap-alt-scroll-writer
  [wrtr options & body]
  `(binding [*out* (get-writer-to-wrap ~wrtr ~options)]
     ~@body))

(defmacro with-alt-scroll-out
  [& body]
  `(wrap-alt-scroll-writer ~'__alt-scroll-wrtr ~'__options ~@body))

(defn windows-workaround
  []
  (when (utils/is-os? "windows")
    (TerminalFactory/registerFlavor TerminalFactory$Flavor/WINDOWS UnsupportedTerminal)))

(defmacro start-cli
  "This is the primary entry point for starting and configuring cli4clj.
   Please note that the configuration options can also be defined in a global or local var.
   However, in order to lookup arguments defined in anonymous functions, the configuration options have to be defined directly in the macro call."
  [user-options]
  `(let [~'__options (atom nil)
         ~'__alt-scroll-wrtr (atom nil)]
     (windows-workaround)
     (reset! ~'__options (assoc
                          (get-cli-opts (add-args-info-m ~user-options))
                          :calling-ns ~*ns*))
     (reset! ~'__alt-scroll-wrtr (alt-scroll-writer @~'__options))
     (wrap-alt-scroll-writer
      ~'__alt-scroll-wrtr
      ~'__options
      (main/repl
       :eval ((@~'__options :eval-factory) @~'__options)
       :print (@~'__options :print)
       :prompt (@~'__options :prompt-fn)
       :read (*read-factory* @~'__options)))
     (when (@~'__options :alternate-scrolling)
       (print (str "\u001b[r\u001b[" (-> (TerminalFactory/create) (.getHeight)) ";0H")))))

(defn create-embedded-read-fn
  "This creates a read fn intended for use in the embedded CLI."
  [opts in-chan]
  (let [rdr-fn (create-repl-read-fn opts)]
    (fn [request-prompt request-exit]
      (let [#^java.lang.String line (async/<!! in-chan)]
        (if (and (not (nil? line))
                 (not (.isEmpty line))
                 (not (-> line (.trim) (.startsWith *comment-begin-string*))))
          (binding [*in* (PushbackReader. (StringReader. (str line *line-sep*)))]
            (rdr-fn request-prompt request-exit))
          request-prompt)))))

(defmacro embedded-cli-fn
  "Create an embedded CLI.
   The embedded CLI is different from the classical CLI that can be created via start-cli
   in the sense that does not aim at providing an interactive CLI that can be used via a command prompt.
   An example for a use case of the embedded CLI is as part of a client-server application for which commands
   can be executed, e.g., remotely on the server from the client.
   For such use cases, the embedded CLI aims on easing the CLI definition similarly to start-cli.
   
   This macro accepts the same options map as start-cli.
   It will return a function that accepts string input.
   The returned function accepts a single string argument that corresponds to the CLI input.
   It returns the string resulting from evaluating the provided input."
  [user-options]
  `(let [out-wrtr# (atom (java.io.StringWriter.))
         out-chan# (async/chan)
         prompt-fn# (fn []
                      (let [out-str# (-> (str @out-wrtr#) (.trim))]
                        (reset! out-wrtr# (java.io.StringWriter.))
                        (when (not (-> out-str# (.isEmpty)))
                          (async/>!! out-chan# (str out-str#)))
                        nil))
         adjusted-user-options# (merge-options
                                  {}
                                  ~user-options
                                  {:prompt-fn prompt-fn#})
         in-chan# (async/chan)
         in-fn# (fn [input#]
                  (async/>!! in-chan# input#)
                  (async/<!! out-chan#))
         read-factory# (fn [opts#] (create-embedded-read-fn opts# in-chan#))]
     (doto
       (Thread. #(loop []
                   (binding [*read-factory* read-factory#]
                     (utils/with-err-str-cb
                       (fn [err#]
                         (when
                           (and
                             (not (nil? err#))
                             (not (-> (str err#) (.trim) (.isEmpty))))
                           (.write ^java.io.Writer @out-wrtr# (str "ERROR: " err#))))
                     (utils/with-out-str-cb
                       (fn [out#]
                         (condp = (type out#)
                           String (when (not-empty out#) (.write ^java.io.Writer @out-wrtr# ^String out#))
                           Integer (when (not (nil? out#)) (.write ^java.io.Writer @out-wrtr# ^Integer out#))))
                       (start-cli adjusted-user-options#))))
                   (recur)))
       (.setDaemon true)
       (.setName "cli4clj Embedded CLI")
       (.start))
     in-fn#))


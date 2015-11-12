;;;
;;;   Copyright 2015 Ruediger Gad
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
    [clojure.string :only [blank? split]]
    clj-assorted-utils.util)
  (:import
    (java.io PushbackReader StringReader)
    (jline.console ConsoleReader)
    (jline.console.completer ArgumentCompleter Completer StringsCompleter)))

(defn cli-repl-print
  [arg]
  (if (not (nil? arg))
    (prn arg)))

(defn create-repl-read-fn
  [cmds]
  "The created read function is largely based on the exisiting repl read function:
   http://clojure.github.io/clojure/clojure.main-api.html#clojure.main/repl-read
   The main difference is that if the first argument on a line is a keyword,
   all elements on that line will be forwarded in a vector instead of being
   forwarded seperately."
  (fn [request-prompt request-exit]
    (or ({:line-start request-prompt :stream-end request-exit}
         (skip-whitespace *in*))
        (loop [v []]
          (let [input (read {:read-cond :allow} *in*)]
            (if (and (not (symbol? input)) (empty? v))
              (do
                (skip-if-eol *in*)
                input)
              (if (= :line-start (skip-whitespace *in*))
                (conj v input)
                (recur (conj v input)))))))))

(defn get-cmd-aliases
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

(defn create-arg-hint-completers
  [cmds cmd-aliases]
  (reduce
    (fn [v k]
      (let [fn-args (get-in cmds [k :fn-args])
            completion-hint (get-in cmds [k :completion-hint])]
        (if (or (not (nil? fn-args))
                  (not (nil? completion-hint)))
          (conj v
                (ArgumentCompleter.
                  [(StringsCompleter. (conj (vec (map name (cmd-aliases k))) (name k)))
                   (proxy [Completer] []
                     (complete [buffer cursor candidates]
                       (if (not (nil? fn-args))
                         (.add candidates (str "Arguments: "
                                               fn-args
                                               (if (not (nil? completion-hint))
                                                 "\n"))))
                       (if (not (nil? completion-hint))
                         (.add candidates (str completion-hint)))
                       (.add candidates "")
                       0))]))
          v)))
    []
    (keys cmds)))

(defn create-jline-read-fn
  [cmds prompt-string]
  (let [cmd-aliases (get-cmd-aliases cmds)
        in-rdr (doto (ConsoleReader.)
                 (.addCompleter (StringsCompleter. (map name (keys cmds))))
                 (.setPrompt prompt-string))
        arg-hint-completers (create-arg-hint-completers cmds cmd-aliases)
        _ (doseq [compl arg-hint-completers]
            (.addCompleter in-rdr compl))
        rdr-fn (create-repl-read-fn cmds)]
    (fn [request-prompt request-exit]
      (let [line (.readLine in-rdr)]
        (if (and (not (nil? line))
                 (not (.isEmpty line))
                 (not (-> line (.trim) (.startsWith ";"))))
          (binding [*in* (PushbackReader. (StringReader. (str line "\n")))]
            (rdr-fn request-prompt request-exit))
          request-prompt)))))

(defn resolve-cmd-alias
  [input-cmd cmds]
  (if (keyword? (cmds input-cmd))
    (cmds input-cmd)
    input-cmd))

(defn create-cli-eval-fn
  [cmds allow-eval print-err]
  (fn [arg]
    (if (and (vector? arg) (contains? cmds (keyword (first arg))))
      (let [cmd (resolve-cmd-alias (keyword (first arg)) cmds)]
        (try
          (apply
            (get-in cmds [cmd :fn])
            (rest arg))
          (catch Exception e
            (print-err (.getMessage e)))))
      (if allow-eval
        (eval arg)
        (print-err (str "Invalid command: \"" arg "\". Please type \"help\" to get an overview of commands."))))))

(defn create-cli-help-fn
  [options]
  (let [cmds (:cmds options)
        cmd-aliases (get-cmd-aliases cmds)
        command-names (sort (keys cmds))
        cmd-entry-delimiter (:help-cmd-entry-delimiter options)]
    (fn []
      (doseq [c command-names]
        (when (map? (cmds c))
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

(def cli-mandatory-default-options
  {:cmds {:exit {:fn (fn [] (System/exit 0))
                 :short-info "Exit the CLI."
                 :long-info "Terminate and close the command line interface."}
          :help {:short-info "Show help."
                 :long-info "Display a help text that lists all available commands including further detailed information about these commands."}}})

(defmulti print-err-fn (fn [arg] (= (type arg) Exception)))
(defmethod print-err-fn true [arg]
  (println-err (.getMessage arg)))
(defmethod print-err-fn false [arg]
  (println-err (str arg)))

(def cli-default-options
  {:allow-eval false
   :cmds {:e :exit
          :h :help
          :? :help
          :quit :exit
          :q :exit}
   :eval-factory create-cli-eval-fn
   :help-factory create-cli-help-fn
   :help-cmd-entry-delimiter "\n"
   :print cli-repl-print
   :print-err print-err-fn
   :prompt-fn (fn [])
   :prompt-string "cli# "})

(defn merge-options
  [defaults user-options mandatory-defaults]
  (merge-with
    (fn [a b] (if (and (map? a) (map? b))
                (merge a b)
                b))
    defaults user-options mandatory-defaults))

(defn get-cli-opts
  [user-options]
  (let [merged-opts (merge-options cli-default-options user-options cli-mandatory-default-options)
        help-fn ((merged-opts :help-factory) merged-opts)]
    (assoc-in merged-opts [:cmds :help :fn] help-fn)))

(def ^:dynamic *read-factory* create-jline-read-fn)

(defn add-args-info
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
  [opts]
  (add-args-info opts))

(defmacro start-cli
  ([]
    (start-cli {}))
  ([user-options]
    (let [options-with-args-info (add-args-info user-options)]
     `(let [options# (get-cli-opts ~options-with-args-info)]
        (repl
          :eval ((options# :eval-factory) (options# :cmds) (options# :allow-eval) (options# :print-err))
          :print (options# :print)
          :prompt (options# :prompt-fn)
          :read (*read-factory* (options# :cmds) (options# :prompt-string)))))))



(defn cmd-vector-to-test-input-string
  [cmd-vec]
  (reduce (fn [s c] (str s c "\n")) "" cmd-vec))

(defn create-repl-read-test-fn
  [cmds prompt-string]
  (create-repl-read-fn cmds))

(defn exec-tested-fn
  [tested-fn]
  (binding [*read-factory* create-repl-read-test-fn]
    (tested-fn)))

(defn test-cli-stdout
  [tested-fn in-cmds]
  (.trim (with-out-str (with-in-str (cmd-vector-to-test-input-string in-cmds) (exec-tested-fn tested-fn)))))

(defn test-cli-stderr
  [tested-fn in-cmds]
  (.trim (with-err-str (with-out-str (with-in-str (cmd-vector-to-test-input-string in-cmds) (exec-tested-fn tested-fn))))))

(defn expected-string
  ([expected-lines]
    (expected-string expected-lines (System/getProperty "line.separator")))
  ([expected-lines separator]
    (reduce
      (fn [s e] (str s separator e))
      (first expected-lines)
      (rest expected-lines))))


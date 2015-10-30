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
    (jline.console.completer StringsCompleter)))

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

(defn create-jline-read-fn
  [cmds prompt-string]
  (let [in-rdr (doto (ConsoleReader.)
                 (.addCompleter (StringsCompleter. (map name (keys cmds))))
                 (.setPrompt prompt-string))
        rdr-fn (create-repl-read-fn cmds)]
    (fn [request-prompt request-exit]
      (let [line (.readLine in-rdr)]
        (if (and (not (nil? line))
                 (not (.isEmpty line))
                 (not (-> line (.trim) (.startsWith ";"))))
          (binding [*in* (PushbackReader. (StringReader. (str line "\n")))]
            (rdr-fn request-prompt request-exit))
          request-prompt)))))

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
          (if (not (nil? (c cmd-aliases)))
            (println "" (vec (map #(symbol (name %)) (c cmd-aliases))))
            (println ""))
          (when-let [si (get-in cmds [c :short-info])]
            (println (str "\t" si)))
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
   :prompt-string "cli# "
   :read-factory create-jline-read-fn})

(defn merge-options
  [defaults user-options mandatory-defaults]
  (merge-with (fn [a b] (if (and (map? a) (map? b))
                          (merge a b)
                          b))
              defaults user-options mandatory-defaults))

(defn get-cli-opts
  [user-options]
  (let [merged-opts (merge-options cli-default-options user-options cli-mandatory-default-options)]
    (assoc-in merged-opts [:cmds :help :fn] ((merged-opts :help-factory) merged-opts))))

(defn start-cli
  ([]
    (start-cli {}))
  ([user-options]
    (let [options (get-cli-opts user-options)]
      (repl
        :eval ((options :eval-factory) (options :cmds) (options :allow-eval) (options :print-err))
        :print (options :print)
        :prompt (fn [])
        :read ((options :read-factory) (options :cmds) (options :prompt-string))))))



(defn cmd-vector-to-test-input-string
  [cmd-vec]
  (reduce (fn [s c] (str s c "\n")) "" cmd-vec))

(defn create-repl-read-test-fn
  [cmds prompt-string]
  (create-repl-read-fn cmds))

(defn start-test-cli
  [opts]
  (start-cli (assoc-in opts [:read-factory] create-repl-read-test-fn)))

(defn test-cli-stdout
  [cli-opts in-cmds]
  (.trim (with-out-str (with-in-str (cmd-vector-to-test-input-string in-cmds) (start-test-cli cli-opts)))))

(defn test-cli-stderr
  [cli-opts in-cmds]
  (with-err-str (with-out-str (with-in-str (cmd-vector-to-test-input-string in-cmds) (start-test-cli cli-opts)))))


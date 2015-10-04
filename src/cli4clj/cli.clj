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
  (:use [clojure.main :only [repl skip-if-eol skip-whitespace]]
        clj-assorted-utils.util))

(defn cli-repl-print
  [arg]
  (if (not (nil? arg))
    (prn arg)))

(defn cli-repl-prompt
  []
  (print "cli# "))

(defn cli-repl-read
  "This function is largely based on the exisiting repl read function:
   http://clojure.github.io/clojure/clojure.main-api.html#clojure.main/repl-read
   The main difference is that if the first argument on a line is a keyword,
   all elements on that line will be forwarded in a vector instead of being
   forwarded seperately."
  [request-prompt request-exit]
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
               (recur (conj v input))))))))

(defn resolve-cmd-alias
  [input-cmd cmds]
  (if (keyword? (cmds input-cmd))
    (cmds input-cmd)
    input-cmd))

(defn create-cli-eval-fn
  [cmds allow-eval print-err]
  (fn [arg]
;    (println "Eval arg:" arg)
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
  [cmds]
  (fn []
    (let [command-names (sort (keys cmds))]
      (doseq [c command-names]
        (if (map? (cmds c))
          (do
            (println (str (name c) "\t" (get-in cmds [c :short-info])))
            (when-let [li (get-in cmds [c :long-info])]
              (println (str "\t" (get-in cmds [c :long-info])))))
          (println (str (name c) "\tSee: " (name (cmds c)))))))))

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
   :print cli-repl-print
   :print-err print-err-fn
   :prompt cli-repl-prompt
   :read cli-repl-read})

(defn merge-options
  [defaults user-options mandatory-defaults]
  (merge-with (fn [a b] (if (and (map? a) (map? b))
                          (merge a b)
                          b))
              defaults user-options mandatory-defaults))

(defn start-cli
  ([]
    (start-cli {}))
  ([user-options]
    (let [merged-opts (merge-options cli-default-options user-options cli-mandatory-default-options)
          options (assoc-in merged-opts
                    [:cmds :help :fn] ((merged-opts :help-factory) (merged-opts :cmds)))]
      (repl
        :eval ((options :eval-factory) (options :cmds) (options :allow-eval) (options :print-err))
        :print (options :print)
        :prompt (options :prompt)
        :read (options :read)))))



(defn cmd-vector-to-test-input-string
  [cmds]
  (reduce (fn [s c] (str s c "\n")) "" cmds))

(defn test-cli-stdout
  [cli-opts cmds]
  (with-out-str (with-in-str (cmd-vector-to-test-input-string cmds) (start-cli cli-opts))))

(defn test-cli-stderr
  [cli-opts cmds]
  (with-err-str (with-out-str (with-in-str (cmd-vector-to-test-input-string cmds) (start-cli cli-opts)))))


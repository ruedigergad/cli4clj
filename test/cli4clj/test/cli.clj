;;;
;;;   Copyright 2015, 2016 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Tests for cli4clj"}
  cli4clj.test.cli
  (:require
    (cli4clj
      [cli :as cli]
      [cli-tests :as cli-tests])
    (clj-assorted-utils [util :as utils])
    (clojure [test :as test]))
  (:import (java.io ByteArrayInputStream)
           (java.util ArrayList)))

(test/deftest simple-options-merging-test
  (let [user-options {:cmds {:foo {:fn 123}}}
        defaults {:cmds {:bar {:fn 456}}}
        mandatory-defaults {}
        expected {:cmds {:bar {:fn 456}
                         :foo {:fn 123}}}]
    (test/is (= expected (cli/merge-options defaults user-options mandatory-defaults)))))

(test/deftest simple-options-override-test
  (let [user-options {:cmds {:foo {:fn 123}
                             :bar {:fn 789}}}
        defaults {:cmds {:bar {:fn 456}}}
        mandatory-defaults {}
        expected {:cmds {:bar {:fn 789}
                         :foo {:fn 123}}}]
    (test/is (= expected (cli/merge-options defaults user-options mandatory-defaults)))))

(test/deftest simple-mandatory-options-override-test
  (let [user-options {:cmds {:foo {:fn 123}
                             :bar {:fn 789}}}
        defaults {}
        mandatory-defaults {:cmds {:bar {:fn 456}}}
        expected {:cmds {:bar {:fn 456}
                         :foo {:fn 123}}}]
    (test/is (= expected (cli/merge-options defaults user-options mandatory-defaults)))))

(test/deftest user-options-override-non-nested-test
  (let [user-options {:prompt :xyz}
        defaults {:prompt :abc}
        mandatory-defaults {}
        expected {:prompt :xyz}]
    (test/is (= expected (cli/merge-options defaults user-options mandatory-defaults)))))



(test/deftest simple-test-cli-interaction-stdout-test
  (let [out-string (cli-tests/test-cli-stdout #(cli/start-cli {}) [])]
    (test/is (= "" out-string))))

(test/deftest simple-test-cli-interaction-stderr-test
  (let [err-string (cli-tests/test-cli-stderr #(cli/start-cli {}) ["xyz"])]
    (test/is (.startsWith err-string "Invalid command: \"[xyz]\"."))))

(test/deftest add-cmd-cli-interaction-stdout-test
  (let [cli-opts {:cmds {:add {:fn #(+ %1 %2)}}}
        test-cmd-input ["add 1 2"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= "3" out-string))))

(test/deftest add-cmd-cli-interaction-stdout-multiline-test
  (let [cli-opts {:cmds {:add {:fn #(+ %1 %2)}}}
        test-cmd-input ["add 1 2" "add 3 4" "add 5 6"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= (cli-tests/expected-string ["3" "7" "11"]) out-string))))

(test/deftest div-cmd-cli-interaction-cmd-error-test
  (let [cli-opts {:cmds {:div {:fn #(/ %1 %2)}}}
        test-cmd-input ["div 1 0"]
        out-string (cli-tests/test-cli-stderr #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= "Divide by zero" out-string))))

(test/deftest help-cmd-cli-interaction-stdout-test
  (let [cli-opts {:cmds {:add {:fn #(+ %1 %2)
                               :fn-args "fn-args string"}}}
        test-cmd-input ["help"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= (str "add"
                " -- Arguments: fn-args string" cli/*line-sep* cli/*line-sep* cli/*line-sep*
                "help [? h]" cli/*line-sep*
                "\tShow help." cli/*line-sep*
                "\tDisplay a help text that lists all available commands including further detailed information about these commands." cli/*line-sep* cli/*line-sep*
                "quit [q]" cli/*line-sep*
                "\tQuit the CLI." cli/*line-sep*
                "\tTerminate and close the command line interface.")
           out-string))))

(test/deftest allow-eval-cli-interaction-test
  (let [cli-opts {:allow-eval true}
        test-cmd-input ["(+ 1 2)"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= (cli-tests/expected-string ["3"]) out-string))))



(test/deftest get-cmd-aliases-test
  (let [cmds {:a {}
              :b :a
              :c :a
              :d {}
              :foo :a
              :y :d
              :x :d}
        expected {:a [:b :c :foo]
                  :d [:x :y]}]
    (test/is (= expected (cli/get-cmd-aliases cmds)))))



(test/deftest get-args-info-simple-fns-test
  (let [result (cli/add-args-info-m {:cmds {:a {:fn (fn [arg] (inc arg))}
                                            :b {:fn (fn [summand1 summand2] (+ summand1 summand2))}}})]
    (test/is (= '[[arg]] (get-in result [:cmds :a :fn-args])))
    (test/is (= '[[summand1 summand2]] (get-in result [:cmds :b :fn-args])))))

(test/deftest get-args-info-simple-multi-arity-fn-test
  (let [result (cli/add-args-info-m {:cmds {:a {:fn (fn ([a] (inc a)) ([a b] (+ a b)) ([a b c] (+ a b c)))}}})]
    (test/is (= '[[a] [a b] [a b c]] (get-in result [:cmds :a :fn-args])))))

(defn test-fn-single-arity
  [summand1 summand2]
  (+ summand1 summand2))

(defn test-fn-multi-arity
  ([a] (inc a))
  ([a b] (+ a b))
  ([a b c] (+ a b c)))

(test/deftest get-args-info-defns-test
  (let [result (cli/add-args-info-m {:cmds {:a {:fn test-fn-multi-arity}
                                        :b {:fn test-fn-single-arity}}})]
    (test/is (= '[[a] [a b] [a b c]] (get-in result [:cmds :a :fn-args])))
    (test/is (= '[[summand1 summand2]] (get-in result [:cmds :b :fn-args])))))

(test/deftest get-args-info-override-test
  (let [result (cli/add-args-info-m {:cmds {:a {:fn (fn [arg] (inc arg))
                                            :fn-args "My Custom Argument Description"}
                                        :b {:fn (fn [summand1 summand2] (+ summand1 summand2))}}})]
    (test/is (= "My Custom Argument Description" (get-in result [:cmds :a :fn-args])))
    (test/is (= '[[summand1 summand2]] (get-in result [:cmds :b :fn-args])))))

(test/deftest simple-create-arg-hint-completers-test
  (let [cmd-map {:a {:fn-args [['x 'y 'z]]}
                 :b {:fn-args "Test hint"}}
        completers (cli/create-arg-hint-completers cmd-map)]
    (test/is (vector? completers))
    (test/is (= 2 (count completers)))))

(test/deftest create-arg-hint-completers-completion-proposal-test
  (let [cmd-map {:a {:fn-args [['x 'y 'z]]}
                 :b {:fn-args "my args"
                     :completion-hint "test hint"}}
        completers (cli/create-arg-hint-completers cmd-map)
        arr-lst (ArrayList.)]
    (test/is (= 2 (.complete (nth completers 0) "a " 2 arr-lst)))
    (test/is (= 2 (.size arr-lst)))
    (test/is (= "Arguments: [[x y z]]" (.get arr-lst 0)))
    (test/is (= "" (.get arr-lst 1)))
    (.clear arr-lst)
    (test/is (= 2 (.complete (nth completers 1) "b " 2 arr-lst)))
    (test/is (= 2 (.size arr-lst)))
    (test/is (= "Arguments: my args" (.get arr-lst 0)))
    (test/is (= "test hint" (.get arr-lst 1)))))

(test/deftest create-arg-hint-completers-link-hint-test
  (let [cmd-map {:a {:foo "Linking to foo hint test."
                     :completion-hint :foo}}
        completers (cli/create-arg-hint-completers cmd-map)
        arr-lst (ArrayList.)]
    (test/is (= 2 (.complete (nth completers 0) "a " 2 arr-lst)))
    (test/is (= 2 (.size arr-lst)))
    (test/is (= "Linking to foo hint test." (.get arr-lst 0)))
    (test/is (= "" (.get arr-lst 1)))))



(test/deftest print-cmd-invalid-token-to-string-fallback-test
  (let [cli-opts {:cmds {:print {:fn #(println %)}}}
        test-cmd-input ["print /foo/bar"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= (cli-tests/expected-string ["/foo/bar"]) out-string))))

(test/deftest print-cmd-invalid-token-exception-test
  (let [cli-opts {:cmds {:print {:fn #(println %)}}
                  :invalid-token-to-string false}
        test-cmd-input ["print /foo/bar"]
        out-string (cli-tests/test-cli-stderr #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (.startsWith out-string "java.lang.RuntimeException: Invalid token: /foo/bar"))))



(test/deftest enable-trace-test
  (let [cli-opts {}
        test-cmd-input ["_enable-trace true"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= (cli-tests/expected-string ["print-exception-trace is set to: true"]) out-string))))

(test/deftest enable-trace-wrong-arg-test
  (let [cli-opts {}
        test-cmd-input ["_enable-trace 123"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= (cli-tests/expected-string
             ["Error, you need to supply a boolean value: true or false"
              "print-exception-trace is set to: false"])
           out-string))))



(test/deftest embedded-cli-in-out-test
  (let [cli-opts {:cmds {:print {:fn #(println %)}
                         :divide {:fn (fn [x y] (/ x y))}}}
        cli-fn (cli/embedded-cli-fn cli-opts)]
    (test/is (= "foo_bar" (cli-fn "print foo_bar")))
    (test/is (= "2" (cli-fn "divide 4 2")))))


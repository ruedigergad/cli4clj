;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Tests for cli4clj"}
  cli4clj.test.cli
  (:use clojure.test
        cli4clj.cli
        clj-assorted-utils.util)
  (:import (java.util ArrayList)))

(deftest simple-options-merging-test
  (let [user-options {:cmds {:foo {:fn 123}}}
        defaults {:cmds {:bar {:fn 456}}}
        mandatory-defaults {}
        expected {:cmds {:bar {:fn 456}
                         :foo {:fn 123}}}]
    (is (= expected (merge-options defaults user-options mandatory-defaults)))))

(deftest simple-options-override-test
  (let [user-options {:cmds {:foo {:fn 123}
                             :bar {:fn 789}}}
        defaults {:cmds {:bar {:fn 456}}}
        mandatory-defaults {}
        expected {:cmds {:bar {:fn 789}
                         :foo {:fn 123}}}]
    (is (= expected (merge-options defaults user-options mandatory-defaults)))))

(deftest simple-mandatory-options-override-test
  (let [user-options {:cmds {:foo {:fn 123}
                             :bar {:fn 789}}}
        defaults {}
        mandatory-defaults {:cmds {:bar {:fn 456}}}
        expected {:cmds {:bar {:fn 456}
                         :foo {:fn 123}}}]
    (is (= expected (merge-options defaults user-options mandatory-defaults)))))

(deftest user-options-override-non-nested-test
  (let [user-options {:prompt :xyz}
        defaults {:prompt :abc}
        mandatory-defaults {}
        expected {:prompt :xyz}]
    (is (= expected (merge-options defaults user-options mandatory-defaults)))))



(deftest cmd-vector-to-cmd-test-input-string-test
  (let [expected "foo\nbar\n"
        cmd-vec ["foo" "bar"]]
    (is (= expected (cmd-vector-to-test-input-string cmd-vec)))))

(deftest simple-test-cli-interaction-stdout-test
  (let [out-string (test-cli-stdout #(start-cli {}) [])]
    (is (= "" out-string))))

(deftest simple-test-cli-interaction-stderr-test
  (let [err-string (test-cli-stderr #(start-cli {}) ["xyz"])]
    (is (.startsWith err-string "Invalid command: \"[xyz]\"."))))

(deftest add-cmd-cli-interaction-stdout-test
  (let [cli-opts {:cmds {:add {:fn #(+ %1 %2)}}}
        test-cmd-input ["add 1 2"]
        out-string (test-cli-stdout #(start-cli cli-opts) test-cmd-input)]
    (is (= "3" out-string))))

(deftest add-cmd-cli-interaction-stdout-multiline-test
  (let [cli-opts {:cmds {:add {:fn #(+ %1 %2)}}}
        test-cmd-input ["add 1 2" "add 3 4" "add 5 6"]
        out-string (test-cli-stdout #(start-cli cli-opts) test-cmd-input)]
    (is (= (expected-string ["3" "7" "11"]) out-string))))

(deftest add-cmd-cli-interaction-cmd-error-test
  (let [cli-opts {:cmds {:div {:fn #(/ %1 %2)}}}
        test-cmd-input ["div 1 0"]
        out-string (test-cli-stderr #(start-cli cli-opts) test-cmd-input)]
    (is (= "Divide by zero" out-string))))

(deftest help-cmd-cli-interaction-stdout-test
  (let [cli-opts {:cmds {:add {:fn #(+ %1 %2)}}}
        test-cmd-input ["help"]
        out-string (test-cli-stdout #(start-cli cli-opts) test-cmd-input)]
    (is (= "add\n\n\nhelp [? h]\n\tShow help.\n\tDisplay a help text that lists all available commands including further detailed information about these commands.\n\nquit [q]\n\tQuit the CLI.\n\tTerminate and close the command line interface."
           out-string))))

(deftest allow-eval-cli-interaction-test
  (let [cli-opts {:allow-eval true}
        test-cmd-input ["(+ 1 2)"]
        out-string (test-cli-stdout #(start-cli cli-opts) test-cmd-input)]
    (is (= (expected-string ["3"]) out-string))))



(deftest expected-string-creation-single-line-test
  (let [in ["a"]
        out (expected-string in)]
    (is (= "a" out))))

(deftest expected-string-creation-multi-line-test
  (let [in ["a" "b" "c"]
        out (expected-string in)
        line-sep (System/getProperty "line.separator")]
    (is (= (str "a" line-sep "b" line-sep "c") out))))

(deftest expected-string-creation-custom-separator-test
  (let [in ["a" "b" "c"]
        out (expected-string in "foo")
        line-sep "foo"]
    (is (= (str "a" line-sep "b" line-sep "c") out))))



(deftest get-cmd-aliases-test
  (let [cmds {:a {}
              :b :a
              :c :a
              :d {}
              :foo :a
              :y :d
              :x :d}
        expected {:a [:b :c :foo]
                  :d [:x :y]}]
    (is (= expected (get-cmd-aliases cmds)))))



(deftest get-args-info-simple-fns-test
  (let [result (add-args-info-m {:cmds {:a {:fn (fn [arg] (inc arg))}
                                        :b {:fn (fn [summand1 summand2] (+ summand1 summand2))}}})]
    (is (= '[[arg]] (get-in result [:cmds :a :fn-args])))
    (is (= '[[summand1 summand2]] (get-in result [:cmds :b :fn-args])))))

(deftest get-args-info-simple-multi-arity-fn-test
  (let [result (add-args-info-m {:cmds {:a {:fn (fn ([a] (inc a)) ([a b] (+ a b)) ([a b c] (+ a b c)))}}})]
    (is (= '[[a] [a b] [a b c]] (get-in result [:cmds :a :fn-args])))))

(defn test-fn-single-arity
  [summand1 summand2]
  (+ summand1 summand2))

(defn test-fn-multi-arity
  ([a] (inc a))
  ([a b] (+ a b))
  ([a b c] (+ a b c)))

(deftest get-args-info-defns-test
  (let [result (add-args-info-m {:cmds {:a {:fn test-fn-multi-arity}
                                        :b {:fn test-fn-single-arity}}})]
    (is (= '[[a] [a b] [a b c]] (get-in result [:cmds :a :fn-args])))
    (is (= '[[summand1 summand2]] (get-in result [:cmds :b :fn-args])))))

(deftest get-args-info-override-test
  (let [result (add-args-info-m {:cmds {:a {:fn (fn [arg] (inc arg))
                                            :fn-args "My Custom Argument Description"}
                                        :b {:fn (fn [summand1 summand2] (+ summand1 summand2))}}})]
    (is (= "My Custom Argument Description" (get-in result [:cmds :a :fn-args])))
    (is (= '[[summand1 summand2]] (get-in result [:cmds :b :fn-args])))))

(deftest simple-create-arg-hint-completers-test
  (let [cmd-map {:a {:fn-args [['x 'y 'z]]}
                 :b {:fn-args "Test hint"}}
        completers (create-arg-hint-completers cmd-map)]
    (is (vector? completers))
    (is (= 2 (count completers)))))

(deftest create-arg-hint-completers-completion-proposal-test
  (let [cmd-map {:a {:fn-args [['x 'y 'z]]}
                 :b {:fn-args "my args"
                     :completion-hint "test hint"}}
        completers (create-arg-hint-completers cmd-map)
        arr-lst (ArrayList.)]
    (is (= 2 (.complete (nth completers 0) "a " 2 arr-lst)))
    (is (= 2 (.size arr-lst)))
    (is (= "Arguments: [[x y z]]" (.get arr-lst 0)))
    (is (= "" (.get arr-lst 1)))
    (.clear arr-lst)
    (is (= 2 (.complete (nth completers 1) "b " 2 arr-lst)))
    (is (= 2 (.size arr-lst)))
    (is (= "Arguments: my args" (.get arr-lst 0)))
    (is (= "test hint" (.get arr-lst 1)))))

(deftest create-arg-hint-completers-link-hint-test
  (let [cmd-map {:a {:foo "Linking to foo hint test."
                     :completion-hint :foo}}
        completers (create-arg-hint-completers cmd-map)
        arr-lst (ArrayList.)]
    (is (= 2 (.complete (nth completers 0) "a " 2 arr-lst)))
    (is (= 2 (.size arr-lst)))
    (is (= "Linking to foo hint test." (.get arr-lst 0)))
    (is (= "" (.get arr-lst 1)))))



(deftest simple-jline-readfn-mock-test
  (let [in-string "a 1\nb 2 3\nq"
        out (binding [*mock-jline-readline-input* true]
              (with-out-str
                (with-in-str in-string
                  (start-cli {:cmds {:a {:fn (fn [arg] (inc arg))}
                                     :b {:fn (fn [summand1 summand2] (+ summand1 summand2))}}}))))]
    (is (= (expected-string ["2" "5"] out)))))


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
        clj-assorted-utils.util))

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



(deftest get-args-info-test
  (let [expected {:cmds {:a {:fn (fn [arg] (inc arg))
                             :fn-args '[[arg]]}
                         :b {:fn (fn [summand1 summand2] (+ summand1 summand2))
                             :fn-args '[[summand1 summand2]]}}}]
    (is (= expected (add-args-info-macro {:cmds {:a {:fn (fn [arg] (inc arg))}
                                                 :b {:fn (fn [summand1 summand2] (+ summand1 summand2))}}})))))


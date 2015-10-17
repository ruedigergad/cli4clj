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

(deftest get-prompt-string-test
  (is (= "cli# " (get-prompt-string {}))))

(deftest simple-test-cli-interaction-stdout-test
  (let [out-string (test-cli-stdout {} [])]
    (is (= "" out-string))))

(deftest simple-test-cli-interaction-stderr-test
  (let [err-string (test-cli-stderr {} ["xyz"])]
    (is (.startsWith err-string "Invalid command: \"[xyz]\"."))))

(deftest add-cmd-cli-interaction-stdout-test
  (let [out-string (test-cli-stdout {:cmds {:add {:fn #(+ %1 %2)}}} ["add 1 2"])]
    (is (= "3" out-string))))



(deftest get-cmd-aliases-test
  (let [cmds {:a {}
              :b :a
              :c :a
              :d {}
              :foo :a
              :y :d
              :x :d}
        expected {:a #{:b :c :foo}
                  :d #{:x :y}}]
    (is (= expected (get-cmd-aliases cmds)))))


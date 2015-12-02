;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Example tests for demonstrating the testing of a main method."}
  cli4clj.test.example
  (:require
    (clojure
      [test :refer :all])
    (cli4clj
      [cli :refer :all]
      [example :refer :all])
    (clj-assorted-utils [util :refer :all])))

(deftest simple-main-method-example-test
  (let [test-cmd-input ["t"]
        out-string (test-cli-stdout #(-main "") test-cmd-input)]
    (is (= "This is a test." out-string))))

(deftest advanced-main-method-example-test
  (let [test-cmd-input ["add 1 2"
                        "divide 4 2"
                        "d 3 2"]
        out-string (test-cli-stdout #(-main "") test-cmd-input)]
    (is (= (expected-string ["3" "2" "3/2"]) out-string))))

(deftest simple-main-method-error-example-test
  (let [test-cmd-input ["divide 4 0"]
        out-string (test-cli-stderr #(-main "") test-cmd-input)]
    (is (= "Divide by zero" out-string))))

(deftest advanced-main-method-error-example-test
  (let [test-cmd-input ["xyz"
                        "d 4 0"]
        out-string (test-cli-stderr #(-main "") test-cmd-input)]
    (is (= (expected-string ["Invalid command: \"[xyz]\". Please type \"help\" to get an overview of commands."
                             "Divide by zero"])
           out-string))))


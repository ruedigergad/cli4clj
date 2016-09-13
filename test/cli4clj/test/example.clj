;;;
;;;   Copyright 2015, 2016 Ruediger Gad
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
      [test :as test])
    (cli4clj
      [cli :as cli]
      [cli-tests :as cli-tests]
      [example :as example])
    (clj-assorted-utils [util :as utils])))

(test/deftest simple-main-method-example-test
  (let [test-cmd-input ["t"]
        out-string (cli-tests/test-cli-stdout #(example/-main "") test-cmd-input)]
    (test/is (= "This is a test." out-string))))

(test/deftest advanced-main-method-example-test
  (let [test-cmd-input ["add 1 2"
                        "divide 4 2"
                        "d 3 2"]
        out-string (cli-tests/test-cli-stdout #(example/-main "") test-cmd-input)]
    (test/is (= (cli-tests/expected-string ["3" "2" "3/2"]) out-string))))

(test/deftest simple-main-method-error-example-test
  (let [test-cmd-input ["divide 4 0"]
        out-string (cli-tests/test-cli-stderr #(example/-main "") test-cmd-input)]
    (test/is (= "Divide by zero" out-string))))

(test/deftest advanced-main-method-error-example-test
  (let [test-cmd-input ["xyz"
                        "d 4 0"]
        out-string (cli-tests/test-cli-stderr #(example/-main "") test-cmd-input)]
    (test/is (= (cli-tests/expected-string ["Invalid command: \"[xyz]\". Please type \"help\" to get an overview of commands."
                             "Divide by zero"])
           out-string))))

(test/deftest main-method-error-with-trace-example-test
  (let [test-cmd-input ["_enable-trace true"
                        "divide 4 0"]
        out-string (cli-tests/test-cli-stdout #(example/-main "") test-cmd-input)]
    (test/is (.startsWith
          out-string
          (cli-tests/expected-string ["print-exception-trace is set to: true"
                            "java.lang.ArithmeticException: Divide by zero"
                            " at clojure.lang.Numbers.divide (Numbers.java"])))))

(test/deftest print-function-test
  (let [test-cmd-input ["print 1701"]
        out-string (cli-tests/test-cli-stdout #(example/-main "") test-cmd-input)]
    (test/is
      (=
        (cli-tests/expected-string
          ["Arg-type: java.lang.Long Arg: 1701"
           "Opt-args: nil"])
        out-string))))

(test/deftest invalid-token-to-string-fallback-test
  (let [test-cmd-input ["print /foo/bar"]
        out-string (cli-tests/test-cli-stdout #(example/-main "") test-cmd-input)]
    (test/is
      (=
        (cli-tests/expected-string
          ["Arg-type: java.lang.String Arg: \"/foo/bar\""
           "Opt-args: nil"])
        out-string))))

(test/deftest print-function-opt-args-test
  (let [test-cmd-input ["print foo bar baz"]
        out-string (cli-tests/test-cli-stdout #(example/-main "") test-cmd-input)]
    (test/is
      (=
        (cli-tests/expected-string
          ["Arg-type: clojure.lang.Symbol Arg: foo"
           "Opt-args: (bar baz)"])
        out-string))))


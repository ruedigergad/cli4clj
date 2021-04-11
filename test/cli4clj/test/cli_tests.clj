;;;
;;;   Copyright 2015-2021 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Tests for Unit Testing CLIs that are created with cli4clj."}
  cli4clj.test.cli-tests
  (:require
    (cli4clj
      [cli :as cli]
      [cli-tests :as cli-tests])
    (clj-assorted-utils [util :as utils])
    (clojure [test :as test]))
  (:import (java.io ByteArrayInputStream)
           (java.util ArrayList)))



(test/deftest cmd-vector-to-cmd-test-input-string-test
  (let [expected (str "foo" cli/*line-sep* "bar" cli/*line-sep*)
        cmd-vec ["foo" "bar"]]
    (test/is (= expected (cli-tests/cmd-vector-to-test-input-string cmd-vec)))))

(test/deftest expected-string-creation-single-line-test
  (let [in ["a"]
        out (cli-tests/expected-string in)]
    (test/is (= "a" out))))

(test/deftest expected-string-creation-multi-line-test
  (let [in ["a" "b" "c"]
        out (cli-tests/expected-string in)
        line-sep (System/getProperty "line.separator")]
    (test/is (= (str "a" line-sep "b" line-sep "c") out))))

(test/deftest expected-string-creation-custom-separator-test
  (let [in ["a" "b" "c"]
        out (cli-tests/expected-string in "foo")
        line-sep "foo"]
    (test/is (= (str "a" line-sep "b" line-sep "c") out))))



(test/deftest custom-test-cli-stdout-test
  (let [cli-opts {:cmds {:foo {:fn (fn [] (print "bar"))}}}
        test-cmd-input ["foo"]
        intercepted-data (atom nil)
        out-string (cli-tests/test-cli-stdout-cb
                     #(cli/start-cli cli-opts)
                     test-cmd-input
                     (fn [d]
                       (reset! intercepted-data d)))]
    (test/is (= (cli-tests/expected-string ["bar"]) out-string))
    (test/is (= "bar" @intercepted-data))))

(test/deftest custom-test-cli-stderr-test
  (let [cli-opts {:cmds {:foo {:fn (fn [] (utils/print-err "bar"))}}}
        test-cmd-input ["foo"]
        intercepted-data (atom nil)
        out-string (cli-tests/test-cli-stderr-cb
                     #(cli/start-cli cli-opts)
                     test-cmd-input
                     (fn [d]
                       (reset! intercepted-data d)))]
    (test/is (= (cli-tests/expected-string ["bar"]) out-string))
    (test/is (= "bar" @intercepted-data))))



(defn- async-test-fn
  []
  (println "Starting...")
  (let [tmp-out *out*]
    (doto
      (Thread.
        (fn []
          (binding [*out* tmp-out]
            (utils/sleep 500)
            (println "Finished."))))
      (.start)))
  (println "Started."))

(test/deftest async-cmd-not-finished-test
  (let [cli-opts {:cmds {:async-foo {:fn async-test-fn}}}
        test-cmd-input ["async-foo"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= (cli-tests/expected-string ["Starting..." "Started."]) out-string))))

(test/deftest async-cmd-sleep-finished-test
  (let [cli-opts {:cmds {:async-foo {:fn async-test-fn}}}
        test-cmd-input ["async-foo" "_sleep 1000"]
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input)]
    (test/is (= (cli-tests/expected-string ["Starting..." "Started." "Finished."]) out-string))))



(test/deftest async-cmd-string-latch-stdout-test
  (let [cli-opts {:cmds {:async-foo {:fn async-test-fn}}}
        test-cmd-input ["async-foo"]
        sl (cli-tests/string-latch ["Finished." cli/*line-sep*])
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input sl)]
    (test/is (= ["Starting..." cli/*line-sep* "Started." cli/*line-sep* "Finished." cli/*line-sep*] (sl)))
    (test/is (= (cli-tests/expected-string ["Starting..." "Started." "Finished."]) out-string))))

(defn- async-test-stderr-fn
  []
  (utils/println-err "Starting...")
  (let [tmp-err *err*]
    (doto
      (Thread.
        (fn []
          (binding [*err* tmp-err]
            (utils/sleep 500)
            (utils/println-err "Finished."))))
      (.start)))
  (utils/println-err "Started."))

(test/deftest async-cmd-string-latch-stderr-test
  (let [cli-opts {:cmds {:async-foo {:fn async-test-stderr-fn}}}
        test-cmd-input ["async-foo"]
        sl (cli-tests/string-latch ["Finished." cli/*line-sep*])
        out-string (cli-tests/test-cli-stderr #(cli/start-cli cli-opts) test-cmd-input sl)]
    (test/is (= ["Starting..." cli/*line-sep* "Started." cli/*line-sep* "Finished." cli/*line-sep*] (sl)))
    (test/is (= (cli-tests/expected-string ["Starting..." "Started." "Finished."]) out-string))))

(test/deftest async-cmd-string-latch-stdout-with-callback-test
  (let [cli-opts {:cmds {:async-foo {:fn async-test-fn}}}
        test-cmd-input ["async-foo"]
        val-0 (atom nil)
        val-1 (atom nil)
        val-2 (atom nil)
        sl (cli-tests/string-latch [["Starting..." #(reset! val-0 %)]
                                    ["Started." (fn [v] (reset! val-1 v))]
                                    ["Finished." #(reset! val-2 %)]
                                    cli/*line-sep*])
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input sl)]
    (test/is (= ["Starting..."] @val-0))
    (test/is (= ["Starting..." cli/*line-sep* "Started."] @val-1))
    (test/is (= ["Starting..." cli/*line-sep* "Started." cli/*line-sep* "Finished."] @val-2))
    (test/is (= (cli-tests/expected-string ["Starting..." "Started." "Finished."]) out-string))))

(test/deftest async-cmd-string-latch-stdout-with-mixed-callback-test
  (let [cli-opts {:cmds {:async-foo {:fn async-test-fn}}}
        test-cmd-input ["async-foo"]
        val-0 (atom nil)
        val-1 (atom nil)
        val-2 (atom nil)
        sl (cli-tests/string-latch ["Starting..."
                                    ["Started."]
                                    ["Finished." #(reset! val-2 %)]
                                    cli/*line-sep*])
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input sl)]
    (test/is (= nil @val-0))
    (test/is (= nil @val-1))
    (test/is (= ["Starting..." cli/*line-sep* "Started." cli/*line-sep* "Finished."] @val-2))
    (test/is (= (cli-tests/expected-string ["Starting..." "Started." "Finished."]) out-string))))



(test/deftest simple-jline-input-stream-mock-test
  (let [in-string (str "a 1" cli/*line-sep* "b 2 3" cli/*line-sep* "q" cli/*line-sep*)
        out (binding [cli/*jline-input-stream* (ByteArrayInputStream. (.getBytes in-string))]
              (with-out-str
                (cli/start-cli {:cmds {:a {:fn (fn [arg] (inc arg))}
                                   :b {:fn (fn [summand1 summand2] (+ summand1 summand2))}}})))]
    (test/is (= (cli-tests/expected-string ["2" (str "5" cli/*line-sep*)]) out))))



(test/deftest clojure-repl-stdout-no-op-test
  (let [in-cmds [""]
        out (cli-tests/test-cli-stdout clojure.main/repl in-cmds)]
    (test/is (= (str *ns* "=> " *ns* "=>") out))))

(test/deftest clojure-repl-stderr-no-op-test
  (let [in-cmds [""]
        out (cli-tests/test-cli-stderr clojure.main/repl in-cmds)]
    (test/is (= "" out))))

(test/deftest clojure-repl-stdout-inc-test
  (let [in-cmds ["(inc 1)"]
        out (cli-tests/test-cli-stdout clojure.main/repl in-cmds)]
    (test/is (= (cli-tests/expected-string [(str *ns* "=> 2") (str *ns* "=>")]) out))))

(test/deftest clojure-repl-stdout-def-inc-println-test
  (let [in-cmds ["(def x 21)" "(inc x)" "(println x)"]
        out (cli-tests/test-cli-stdout clojure.main/repl in-cmds)]
    (test/is (= (cli-tests/expected-string [(str *ns*"=> #'" *ns* "/x") (str *ns* "=> 22") (str *ns* "=> 21") "nil" (str *ns* "=>")]) out))))

(test/deftest clojure-repl-stderr-div-zero-test
  (let [in-cmds ["(/ 1 0)"]
        out (cli-tests/test-cli-stderr clojure.main/repl in-cmds)]
    (test/is (.startsWith out "Execution error (ArithmeticException)"))))

(test/deftest clojure-repl-stdout-no-op-no-prompt-test
  (let [in-cmds [""]
        out (cli-tests/test-cli-stdout #(clojure.main/repl :prompt str) in-cmds)]
    (test/is (= "" out))))

(test/deftest clojure-repl-stdout-inc-no-prompt-test
  (let [in-cmds ["(inc 1)"]
        out (cli-tests/test-cli-stdout #(clojure.main/repl :prompt str) in-cmds)]
    (test/is (= "2" out))))

(test/deftest clojure-repl-stdout-def-inc-println-no-prompt-test
  (let [in-cmds ["(def x 21)" "(inc x)" "(println x)"]
        out (cli-tests/test-cli-stdout #(clojure.main/repl :prompt str) in-cmds)]
    (test/is (= (cli-tests/expected-string [(str "#'" *ns* "/x") "22" "21" "nil"]) out))))

(test/deftest clojure-repl-stdout-def-inc-println-no-prompt-string-latch-test
  (let [in-cmds ["(def x 21)" "(inc x)" "(println x)"]
        val-0 (atom nil)
        val-1 (atom nil)
        val-2 (atom nil)
        sl (cli-tests/string-latch [[(str "#'" *ns* "/x") #(reset! val-0 %)]
                                    ["22" #(reset! val-1 %)]
                                    ["21" #(reset! val-2 %)]
                                    "nil"])
        out (cli-tests/test-cli-stdout #(clojure.main/repl :prompt str) in-cmds sl)]
    (test/is (= [(str "#'" *ns* "/x")] @val-0))
    (test/is (= [(str "#'" *ns* "/x") cli/*line-sep* "22"] @val-1))
    (test/is (= [(str "#'" *ns* "/x") cli/*line-sep* "22" cli/*line-sep* "21"] @val-2))
    (test/is (= (cli-tests/expected-string [(str "#'" *ns* "/x") "22" "21" "nil"]) out))))


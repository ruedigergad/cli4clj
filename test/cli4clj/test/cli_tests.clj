;;;
;;;   Copyright 2015, 2016 Ruediger Gad
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
  (let [expected (str "foo" cli/*cli4clj-line-sep* "bar" cli/*cli4clj-line-sep*)
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
        sl (cli-tests/string-latch ["Finished." "\n"])
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input sl)]
    (test/is (= ["Starting..." "\n" "Started." "\n" "Finished." "\n"] (sl)))
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
        sl (cli-tests/string-latch ["Finished." "\n"])
        out-string (cli-tests/test-cli-stderr #(cli/start-cli cli-opts) test-cmd-input sl)]
    (test/is (= ["Starting..." "\n" "Started." "\n" "Finished." "\n"] (sl)))
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
                                    "\n"])
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input sl)]
    (test/is (= ["Starting..."] @val-0))
    (test/is (= ["Starting..." "\n" "Started."] @val-1))
    (test/is (= ["Starting..." "\n" "Started." "\n" "Finished."] @val-2))
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
                                    "\n"])
        out-string (cli-tests/test-cli-stdout #(cli/start-cli cli-opts) test-cmd-input sl)]
    (test/is (= nil @val-0))
    (test/is (= nil @val-1))
    (test/is (= ["Starting..." "\n" "Started." "\n" "Finished."] @val-2))
    (test/is (= (cli-tests/expected-string ["Starting..." "Started." "Finished."]) out-string))))



(test/deftest simple-jline-input-stream-mock-test
  (let [in-string (str "a 1" cli/*cli4clj-line-sep* "b 2 3" cli/*cli4clj-line-sep* "q" cli/*cli4clj-line-sep*)
        out (binding [cli/*jline-input-stream* (ByteArrayInputStream. (.getBytes in-string))]
              (with-out-str
                (cli/start-cli {:cmds {:a {:fn (fn [arg] (inc arg))}
                                   :b {:fn (fn [summand1 summand2] (+ summand1 summand2))}}})))]
    (test/is (= (cli-tests/expected-string ["2" (str "5" cli/*cli4clj-line-sep*)]) out))))


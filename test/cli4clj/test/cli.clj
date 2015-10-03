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
  cli4clj.test.cli
  (:use clojure.test
        cli4clj.cli))

(deftest test-simple-options-merging
  (let [user-options {:cmds {:foo {:fn 123}}}
        defaults {:cmds {:bar {:fn 456}}}
        mandatory-defaults {}
        expected {:cmds {:bar {:fn 456}
                         :foo {:fn 123}}}]
    (is (= expected (merge-options defaults user-options mandatory-defaults)))))


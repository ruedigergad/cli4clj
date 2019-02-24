(ns cli4clj.test.minimal-example
  (:require
    (clojure [test :as test])
    (cli4clj [cli-tests :as cli-tests]
             [minimal-example :as mini-example])))

(test/deftest example-test
  (let [test-cmd-input ["add 1 2"
                        "divide 3 2"]
        out-string (cli-tests/test-cli-stdout
                     #(mini-example/-main "") test-cmd-input)]
    (test/is (=
               (cli-tests/expected-string ["3" "3/2"])
               out-string))))


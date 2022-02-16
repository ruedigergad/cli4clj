# cli4clj

Create simple interactive CLIs for Clojure applications.

[![Clojars Project](https://img.shields.io/clojars/v/cli4clj.svg)](https://clojars.org/cli4clj)
[![Build Status TravisCI](https://travis-ci.com/ruedigergad/cli4clj.svg?branch=master)](https://travis-ci.com/ruedigergad/cli4clj)
[![Build Status CircleCI](https://circleci.com/gh/ruedigergad/cli4clj.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/ruedigergad/cli4clj.svg?style=shield&circle-token=:circle-token)
[![lein test](https://github.com/ruedigergad/cli4clj/actions/workflows/lein_test.yml/badge.svg)](https://github.com/ruedigergad/cli4clj/actions/workflows/lein_test.yml)
[![Coverage Status](https://coveralls.io/repos/github/ruedigergad/cli4clj/badge.svg?branch=master)](https://coveralls.io/github/ruedigergad/cli4clj?branch=master)

## Overview

The aim of cli4clj is to allow the quick and effortless creation of interactive command line interfaces (CLIs).
It is build on top of [jline2](https://github.com/jline/jline2).
Features of cli4clj are:

* Simple configuration via maps
* Command line history
  * Default configuration for persistent history (since 1.4.0)
* Command line editing
* Tab-completion
  * For command names
  * Hints for selected commands: based on function arguments and custom hints
* Aliases can be used to define alternative command names, e.g., for shortcuts.
* Clojure data types, e.g., vector, list, map, etc., can be used as command arguments.
* Build-in help
* Customizable, similar to the Clojure REPL
* Functionality for testing CLIs via unit tests [\[1\]](https://ruedigergad.com/2016/10/23/cli4clj-1-2-5-improved-testability-of-multi-threaded-command-line-applications-in-clojure/), [\[2\]](https://ruedigergad.com/2016/10/27/unit-testing-arbitrary-command-line-interfaces-cli-with-cli4clj-illustrated-using-the-example-of-the-clojure-repl/)
* ["Embedded CLIs"](https://ruedigergad.com/2017/10/09/cli4clj-version-1-3-2-new-embedded-clis/)

On [my website](http://ruedigergad.com/tag/cli4clj) I wrote some posts about cli4clj in which you can find more verbose information.



## Usage

See https://github.com/ruedigergad/cli4clj/blob/master/src/cli4clj/example.clj for a usage example.

The given examples can be run via "lein run", respectively "lein run -- alt" to show the "alternate" scrolling mode.

First, animated gifs show basic interaction and the alternate scrolling mode.
Afterwards, a static textual example provides some more details.

Basic Interaction:

![](https://github.com/ruedigergad/cli4clj/raw/master/docs/ttyrec/basics_introduction.gif "Animated Basic Interaction Example")

Alternate Scrolling Mode Introduction:

![](https://github.com/ruedigergad/cli4clj/raw/master/docs/ttyrec/alternate_scrolling_introduction.gif "Animated Basic Interaction Example")

Below is an example output that shows running the usage example:

    ~/r/p/c/cli4clj (master=) lein run
    cli# help
    add [a]
        Add two values.	 Arguments: [[summand1 summand2]]

    divide [d]
        Divide two values.	 Arguments: [[numer denom]]
        The first argument will be divided by the second argument.

    help [?]
        Show help.
        Display a help text that lists all available commands including further detailed information about these commands.

    quit [q]
        Quit the CLI.
        Terminate and close the command line interface.

    test-cmd [h t]
        Test Command
        Prints a test message to stdout.

    to-csv
        Seq to CSV	 Arguments: [[data]]
        E.g.: "to-csv [1 2 3]"

    cli# test-cmd
    This is a test.
    cli# t
    This is a test.
    cli# add 1 2
    3
    cli# ; Example for an error due to wrong number of arguments.
    cli# add 1 2 3
    Wrong number of args (3) passed to: example/-main/fn--168
    cli# ; Example for an error due to exception in function.
    cli# divide 1 0
    Divide by zero
    cli# d 4 2
    2
    cli# d 4 3
    4/3
    cli# ; Example to show the use of complexer data types, here, a vector and a list.
    cli# to-csv [1 7 0 1]
    "1,7,0,1"
    cli# ; Note that the list is not quoted.
    cli# to-csv (1 8 6 4)
    "1,8,6,4"
    cli# 
    cli# ; Examples of tab-completion.
    cli# TAB
    ?        a        add      d        divide   e        exit     h        help     q        quit     t        test     to-csv   
    cli# add TAB
    Arguments: [[summand1 summand2]]                                      
    cli# add 1 2
    3
    cli# diTAB
    cli# divide TAB
    Arguments: [[numer denom]]   Divide two values.                                        
    cli# divide 1 3
    1/3
    cli# to-csv TAB
    Arguments: [[data]]                                                                                                     
    The data argument can be of any Clojure sequence type, e.g., [1 2 3] or (:a :b :c). Note that the list is not quoted.   
                                                                                                                            
    cli# to-csv [1 2 3]
    "1,2,3"
    cli# q
    ~/r/p/c/cli4clj (master=) lein run


For examples how the testing functionality can be used please see the test cases in: https://github.com/ruedigergad/cli4clj/blob/master/test/cli4clj/test/example.clj

API docs can be found at: http://ruedigergad.github.io/cli4clj/doc/

## Detailed Test Results

Detailed unit test results are avilable at: http://ruedigergad.github.io/cli4clj/test-results/html/

## License

Copyright © 2015-2021 cli4clj Contributors

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.


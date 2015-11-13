# cli4clj

Create simple interactive CLIs for Clojure applications.

[![Clojars Project](http://clojars.org/cli4clj/latest-version.svg)](http://clojars.org/cli4clj)

[![Build Status](https://travis-ci.org/ruedigergad/cli4clj.svg?branch=master)](https://travis-ci.org/ruedigergad/cli4clj)


## Overview

The aim of cli4clj is to allow the quick and effortless creation of interactive command line interfaces (CLIs).
Features of cli4clj are:

* Simple configuration via maps
* Command line history
* Command line editing
* Tab-completion
  * For command names
  * Hints for selected commands: based on function arguments and custom hints
* Aliases can be used to define alternative command names, e.g., for shortcuts.
* Clojure data types, e.g., vector, list, map, etc., can be used as command arguments.
* Build-in help
* Customizable, similar to the Clojure REPL
* Functionality for testing CLIs via unit tests

On [my website](http://ruedigergad.com/tag/cli4clj) I wrote some posts about cli4clj in which you can find more verbose information.



## Usage

See https://github.com/ruedigergad/cli4clj/blob/master/src/cli4clj/example.clj for a usage example.

The given example can be run via "lein run".

Below is an example output that shows running the usage example:

    ~/r/p/c/cli4clj (master=) lein run
    Invalid command: "[xyz]". Please type "help" to get an overview of commands.
    cli# help
    add [a]
        Add two values.	 Arguments: [[summand1 summand2]]

    divide [d]
        Divide two values.	 Arguments: [[numer denom]]
        The first argument will be divided by the second argument.

    exit [e q quit]
        Exit the CLI.
        Terminate and close the command line interface.

    help [?]
        Show help.
        Display a help text that lists all available commands including further detailed information about these commands.

    test [h t]
        Test Command
        Prints a test message to stdout.

    to-csv
        Seq to CSV	 Arguments: [[data]]
        E.g.: "to-csv [1 2 3]"

    cli# test
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


## License

Copyright © 2015 Ruediger Gad

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.


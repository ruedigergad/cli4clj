# cli4clj

Create simple interactive CLIs for Clojure applications.

[![Clojars Project](http://clojars.org/cli4clj/latest-version.svg)](http://clojars.org/cli4clj)

## Usage

See https://github.com/ruedigergad/cli4clj/blob/master/src/cli4clj/example.clj for a usage example.

The given example can be run via "lein run".

Below is an example output that shows running the usage example:

    [rc@localhost cli4clj]$ lein run
    cli# xyz 
    Invalid command: "[xyz]". Please type "help" to get an overview of commands.
    cli# help
    ?       See: help
    a       See: add
    add     Add two values.
    d       See: divide
    divide  Divide two values.
        The first argument will be divided by the second argument.
    e       See: exit
    exit    Exit the CLI.
        Terminate and close the command line interface.
    h       See: test
    help    Show help.
        Display a help text that lists all available commands including further detailed information about these commands.
    q       See: exit
    quit    See: exit
    t       See: test
    test    Test Command
        Prints a test message to stdout.
    to-csv  Seq to CSV
    cli# test
    This is a test.
    cli# t
    This is a test.
    cli# add 1 2
    3
    cli# ; Example of error due to wrong number of arguments.
    cli# add 1 2 3
    ArityException Wrong number of args (3) passed to: example/-main/fn--65  clojure.lang.AFn.throwArity (AFn.java:429)
    cli# ; Example of error due to exception in function.
    cli# divide 1 0
    ArithmeticException Divide by zero  clojure.lang.Numbers.divide (Numbers.java:158)
    cli# d 4 2
    2
    cli# ; Example to show the use of complexer data types, here, a vector and a list.
    cli# to-csv [1 7 0 1]
    "1,7,0,1"
    cli# ; Note that the list is not quoted.
    cli# to-csv (1 8 6 4)
    "1,8,6,4"
    cli# q
    [rc@localhost cli4clj]$

## License

Copyright © 2015 Ruediger Gad

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

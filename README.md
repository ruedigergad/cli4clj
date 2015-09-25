# cli4clj

Create simple interactive CLIs for Clojure applications.

## Usage

See https://github.com/ruedigergad/cli4clj/blob/master/src/cli4clj/example.clj for a usage example.

The given example can be run via "lein run".

Below is an example output that shows running the usage example:

    [rc@localhost cli4clj]$ lein run
    cli# help 
    ?       See: help
    a       See: add
    add
    e       See: exit
    exit    Exit the CLI.
        Terminate and close the command line interface.
    h       See: test
    help    Show help.
        Display a help text that lists all available commands including further detailed information about these commands.
    m       See: multiply
    multiply
    q       See: exit
    quit    See: exit
    t       See: test
    test    Test Command
        Prints a test message to stdout.
    cli# test
    This is a test.
    cli# t
    This is a test.
    cli# add 2 3
    5
    cli# a 2 3
    5
    cli# m 2 3
    6
    cli# q
    [rc@localhost cli4clj]$

## License

Copyright © 2015 Ruediger Gad

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

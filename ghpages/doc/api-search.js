$(function() {
  var index = [
    {label: "cli4clj.cli/*comment-begin-string*", value: "cli4clj.cli.html#IDMULcomment-begin-stringMUL"},
    {label: "cli4clj.cli/*mock-jline-readline-input*", value: "cli4clj.cli.html#IDMULmock-jline-readline-inputMUL"},
    {label: "cli4clj.cli/*read-factory*", value: "cli4clj.cli.html#IDMULread-factoryMUL"},
    {label: "cli4clj.cli/add-args-info", value: "cli4clj.cli.html#IDadd-args-info"},
    {label: "cli4clj.cli/add-args-info-m", value: "cli4clj.cli.html#IDadd-args-info-m"},
    {label: "cli4clj.cli/cli-default-options", value: "cli4clj.cli.html#IDcli-default-options"},
    {label: "cli4clj.cli/cli-mandatory-default-options", value: "cli4clj.cli.html#IDcli-mandatory-default-options"},
    {label: "cli4clj.cli/cli-repl-print", value: "cli4clj.cli.html#IDcli-repl-print"},
    {label: "cli4clj.cli/cmd-vector-to-test-input-string", value: "cli4clj.cli.html#IDcmd-vector-to-test-input-string"},
    {label: "cli4clj.cli/create-arg-hint-completers", value: "cli4clj.cli.html#IDcreate-arg-hint-completers"},
    {label: "cli4clj.cli/create-cli-eval-fn", value: "cli4clj.cli.html#IDcreate-cli-eval-fn"},
    {label: "cli4clj.cli/create-cli-help-fn", value: "cli4clj.cli.html#IDcreate-cli-help-fn"},
    {label: "cli4clj.cli/create-jline-read-fn", value: "cli4clj.cli.html#IDcreate-jline-read-fn"},
    {label: "cli4clj.cli/create-repl-read-fn", value: "cli4clj.cli.html#IDcreate-repl-read-fn"},
    {label: "cli4clj.cli/create-repl-read-test-fn", value: "cli4clj.cli.html#IDcreate-repl-read-test-fn"},
    {label: "cli4clj.cli/exec-tested-fn", value: "cli4clj.cli.html#IDexec-tested-fn"},
    {label: "cli4clj.cli/expected-string", value: "cli4clj.cli.html#IDexpected-string"},
    {label: "cli4clj.cli/get-cli-opts", value: "cli4clj.cli.html#IDget-cli-opts"},
    {label: "cli4clj.cli/get-cmd-aliases", value: "cli4clj.cli.html#IDget-cmd-aliases"},
    {label: "cli4clj.cli/merge-options", value: "cli4clj.cli.html#IDmerge-options"},
    {label: "cli4clj.cli/print-err-fn", value: "cli4clj.cli.html#IDprint-err-fn"},
    {label: "cli4clj.cli/resolve-cmd-alias", value: "cli4clj.cli.html#IDresolve-cmd-alias"},
    {label: "cli4clj.cli/start-cli", value: "cli4clj.cli.html#IDstart-cli"},
    {label: "cli4clj.cli/test-cli-stderr", value: "cli4clj.cli.html#IDtest-cli-stderr"},
    {label: "cli4clj.cli/test-cli-stdout", value: "cli4clj.cli.html#IDtest-cli-stdout"},
    {label: "cli4clj.example/-main", value: "cli4clj.example.html#ID-main"},
    {label: "cli4clj.example/divide", value: "cli4clj.example.html#IDdivide"}  ];
  $('#api-search').autocomplete({
     source: index,
     focus: function(event, ui) {
       event.preventDefault();
     },
     select: function(event, ui) {
       window.open(ui.item.value, '_self');
       ui.item.value = '';
     }
  });
});


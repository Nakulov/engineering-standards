package io.github.nakulov.standards.task.github

import io.github.nakulov.standards.task.shell.ShellResult

fun makeViewArgs(): List<String> {
    return listOf("issue", "view", "13", "--json", "title", "--jq", ".title")
}

fun makeViewCommand(): List<String> {
    return listOf("gh", "issue", "view", "13", "--json", "title", "--jq", ".title")
}

fun makeEditArgs(): List<String> {
    return listOf("issue", "edit", "13", "--add-label", "type:task")
}

fun makeEditCommand(): List<String> {
    return listOf("gh", "issue", "edit", "13", "--add-label", "type:task")
}

fun makeTitle(): String {
    return "Скрипты задачи"
}

fun makeTitleResult(): ShellResult {
    return ShellResult(code = 0, out = "Скрипты задачи\n", err = "")
}

fun makeBadGateway(): ShellResult {
    return ShellResult(code = 1, out = "", err = "HTTP 502: 502 Bad Gateway (https://api.github.com/graphql)\n")
}

fun makeBadGatewayMessage(): String {
    return "gh: HTTP 502: 502 Bad Gateway (https://api.github.com/graphql) — повторите позже"
}

fun makeNotFound(): ShellResult {
    return ShellResult(code = 1, out = "", err = "GraphQL: Could not resolve to an issue with the number of 13.\n")
}

fun makeNotFoundMessage(): String {
    return "gh: GraphQL: Could not resolve to an issue with the number of 13."
}

fun makeRestListCommand(): List<String> {
    return listOf("gh", "api", "--paginate", "repos/owner/repo/issues?state=open&per_page=100", "--jq", ".[].number")
}

fun makePagedQuery(): String {
    return "query(\$n:Int!,\$endCursor:String){user(login:\"owner\"){projectV2(number:\$n){" +
        "items(first:100,after:\$endCursor){nodes{id} pageInfo{hasNextPage endCursor}}}}}"
}

fun makeGraphqlListCommand(): List<String> {
    return listOf(
        "gh",
        "api",
        "graphql",
        "--paginate",
        "-f",
        "query=${makePagedQuery()}",
        "-F",
        "n=10",
        "--jq",
        ".data.user.projectV2.items.nodes[].id",
    )
}

fun makeUnpagedQuery(): String {
    return "query{user(login:\"owner\"){projectV2(number:10){items(first:100){nodes{id}}}}}"
}

fun makeUnpagedQueryMessage(): String {
    return "GraphQL-список без \$endCursor и pageInfo читает только первую страницу"
}

/** Вывод `--paginate` по двум страницам — строки идут подряд, в конце перевод строки. */
fun makeIdsResult(): ShellResult {
    return ShellResult(code = 0, out = "PVTI_1\nPVTI_2\nPVTI_101\n", err = "")
}

fun makeIds(): List<String> {
    return listOf("PVTI_1", "PVTI_2", "PVTI_101")
}

fun makeEmptyResult(): ShellResult {
    return ShellResult(code = 0, out = "", err = "")
}

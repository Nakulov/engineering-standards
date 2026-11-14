package io.github.nakulov.standards.task.github

import io.github.nakulov.standards.task.cli.TaskError

fun makeStateArgs(): List<String> {
    return listOf("issue", "view", "13", "--json", "state,labels", "--jq", makeStateJq())
}

fun makeStateJq(): String {
    return "[.state, ([.labels[].name] | join(\",\"))] | @tsv"
}

fun makeBlockersPath(): String {
    return "repos/{owner}/{repo}/issues/13/dependencies/blocked_by"
}

fun makeBlockersJq(): String {
    return ".[] | select(.state == \"open\") | .number"
}

fun makeNodeIdArgs(): List<String> {
    return listOf("issue", "view", "13", "--json", "id", "--jq", ".id")
}

fun makePullRequestArgs(): List<String> {
    return listOf("pr", "view", "task/13-x", "--json", "number,state", "--jq", "[.number, .state] | @tsv")
}

fun makeOpenTaskInfo(): IssueInfo {
    return IssueInfo(number = 13, open = true, type = "type:task", openBlockers = listOf(3, 8))
}

fun makeClosedUntypedInfo(): IssueInfo {
    return IssueInfo(number = 13, open = false, type = null, openBlockers = emptyList())
}

fun makeNoPullRequestFailure(): TaskError.Failure {
    return TaskError.Failure(message = "gh: no pull requests found for branch \"task/13-x\"")
}

fun makeBadGatewayFailure(): TaskError.Failure {
    return TaskError.Failure(message = makeBadGatewayMessage())
}

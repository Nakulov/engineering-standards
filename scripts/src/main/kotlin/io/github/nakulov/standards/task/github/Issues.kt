package io.github.nakulov.standards.task.github

import io.github.nakulov.standards.task.cli.TaskError

/**
 * Чтение задач и PR текущего репозитория через `gh` (`{owner}/{repo}` подставляет сам `gh`).
 */
class Issues(
    private val gitHub: GitHub,
) {
    fun info(number: Int): IssueInfo {
        val fields = gitHub.read(
            args = listOf("issue", "view", "$number", "--json", "state,labels", "--jq", STATE_AND_LABELS),
        ).split("\t")
        val blockers = gitHub.listRest(
            path = "repos/{owner}/{repo}/issues/$number/dependencies/blocked_by",
            jq = ".[] | select(.state == \"open\") | .number",
        )
        return IssueInfo(
            number = number,
            open = fields.first() == "OPEN",
            type = fields.last().split(",").firstOrNull { label -> label.startsWith(prefix = "type:") },
            openBlockers = blockers.map { blocker -> blocker.toInt() },
        )
    }

    /** Глобальный id задачи — для GraphQL-запросов доски. */
    fun nodeId(number: Int): String {
        return gitHub.read(args = listOf("issue", "view", "$number", "--json", "id", "--jq", ".id"))
    }

    /** Последний PR ветки или `null`, если PR нет. */
    fun pullRequest(branch: String): PullRequest? {
        val fields = readPullRequest(branch = branch) ?: return null
        return PullRequest(number = fields.first().toInt(), merged = fields.last() == "MERGED")
    }

    private fun readPullRequest(branch: String): List<String>? {
        val args = listOf("pr", "view", branch, "--json", "number,state", "--jq", NUMBER_AND_STATE)
        return try {
            gitHub.read(args = args).split("\t")
        } catch (error: TaskError.Failure) {
            if (NO_PR !in error.message.orEmpty()) throw error
            null
        }
    }

    private companion object {
        const val STATE_AND_LABELS = "[.state, ([.labels[].name] | join(\",\"))] | @tsv"
        const val NUMBER_AND_STATE = "[.number, .state] | @tsv"
        const val NO_PR = "no pull requests found"
    }
}

package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.BranchName
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues
import io.github.nakulov.standards.task.github.PullRequests

/**
 * `task pr` из ветки задачи: черновик PR в `dev` (или в long-ветку, от которой начали) с `Closes #N` — или
 * `Part of #N` с `--part` и всегда при базе long-ветке. `task pr --ready` — из черновика; In review — только если PR
 * закрывает задачу, иначе статус остаётся In progress.
 */
class PrCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
    private val pullRequests: PullRequests,
    private val publisher: Publisher,
) : Command {
    override val usage: String = "--title «…» --body-file <файл> [--check «команда»]… [--part] | --ready — " +
        "черновик PR по шаблону; --ready — к ревью"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val name = git.currentBranch()
        val branch = BranchName.parse(branch = name)
            ?: throw TaskError.Refusal(message = "«$name» — не ветка задачи: task pr запускается из task/<N>-…")
        if (branch.long) {
            throw TaskError.Refusal(message = "из long-ветки PR в dev открывает task final ${branch.issue}")
        }
        if (args == listOf(READY)) {
            ready(name = name, issue = branch.issue, console = console)
        } else {
            val base = git.base(branch = name) ?: DEV
            val part = PART in args || base != DEV
            val link = if (part) "Part of #${branch.issue}" else "Closes #${branch.issue}"
            val draft = PullRequestDraft.of(
                args = args - PART,
                head = name,
                base = base,
                link = link,
                draft = true,
                usage = "task pr $usage",
            )
            publisher.publish(draft = draft, console = console)
        }
    }

    private fun ready(
        name: String,
        issue: Int,
        console: Console,
    ) {
        val pullRequest = issues.pullRequest(branch = name)
            ?: throw TaskError.Refusal(message = "у ветки $name нет PR — сначала task pr --title … --body-file …")
        if (pullRequest.merged) {
            throw TaskError.Refusal(message = "PR #${pullRequest.number} уже слит — task done")
        }
        pullRequests.ready(head = name)
        console.line(text = "PR #${pullRequest.number} готов к ревью")
        if (pullRequests.link(head = name).startsWith(prefix = "Closes")) {
            val item = board.item(issue = issue, nodeId = issues.nodeId(number = issue))
            board.move(item = item, status = Board.IN_REVIEW)
            console.line(text = "#$issue → ${Board.IN_REVIEW}")
        } else {
            console.line(text = "#$issue — PR не последний (Part of), статус остаётся")
        }
    }

    private companion object {
        const val READY = "--ready"
        const val PART = "--part"
        const val DEV = "dev"
    }
}

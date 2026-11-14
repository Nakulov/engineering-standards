package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.BranchName
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues

/**
 * `task done` из ветки задачи после слияния её PR: свежий `dev` (в worktree — отсоединение на `origin/dev`, `dev` не
 * трогается), локальная ветка удаляется, задача → Done. Задача ещё открыта (PR с `Part of`) — статус не меняется.
 */
class DoneCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
) : Command {
    override val usage: String = "— после слияния PR: свежий dev, ветка удалена, статус Done"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        if (args.isNotEmpty()) {
            throw TaskError.Failure(message = "использование: task done $usage")
        }
        val name = git.currentBranch()
        val shown = name.ifEmpty { DETACHED }
        val branch = BranchName.parse(branch = name) ?: throw TaskError.Refusal(
            message = "$shown — не ветка задачи: task done запускается из task/<N>-… или long/<N>-…",
        )
        if (!git.isClean()) {
            throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
        }
        val pullRequest = issues.pullRequest(branch = name)
            ?: throw TaskError.Refusal(message = "у ветки $name нет PR — сначала task pr")
        if (!pullRequest.merged) {
            throw TaskError.Refusal(message = "PR #${pullRequest.number} не слит — сначала слияние")
        }
        leaveBranch(name = name, console = console)
        finishIssue(issue = branch.issue, console = console)
    }

    private fun leaveBranch(
        name: String,
        console: Console,
    ) {
        git.fetch()
        if (git.isWorktree()) {
            git.detach(ref = BASE)
            console.line(text = "worktree отсоединён на $BASE, dev не тронут")
        } else {
            git.updateDev()
            console.line(text = "dev обновлён до $BASE")
        }
        git.deleteBranch(name = name)
        console.line(text = "ветка $name удалена")
    }

    private fun finishIssue(
        issue: Int,
        console: Console,
    ) {
        if (issues.info(number = issue).open) {
            console.line(text = "#$issue открыта — не последний PR задачи, статус остаётся")
            return
        }
        val item = board.item(issue = issue, nodeId = issues.nodeId(number = issue))
        if (item.status != Board.DONE) {
            board.move(item = item, status = Board.DONE)
        }
        console.line(text = "#$issue → ${Board.DONE}")
    }

    private companion object {
        const val BASE = "origin/dev"
        const val DETACHED = "HEAD отсоединён"
    }
}

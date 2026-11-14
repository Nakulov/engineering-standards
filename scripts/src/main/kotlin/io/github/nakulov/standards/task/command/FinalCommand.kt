package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.BranchName
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues

/**
 * `task final <N>` из `long/<N>-…`: итоговый PR в `dev` с `Closes #N`, сразу к ревью (части уже проверены), In review.
 */
class FinalCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
    private val publisher: Publisher,
) : Command {
    override val usage: String =
        "<N> --title «…» --body-file <файл> [--check «команда»]… — итоговый PR long/<N>-… → dev"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val issue = args.firstOrNull()?.toIntOrNull()
            ?: throw TaskError.Failure(message = "использование: task final $usage")
        val name = git.currentBranch()
        val branch = BranchName.parse(branch = name)
        if (branch == null || !branch.long || branch.issue != issue) {
            throw TaskError.Refusal(message = "task final $issue запускается из long/$issue-…, сейчас «$name»")
        }
        val draft = PullRequestDraft.of(
            args = args.drop(n = 1),
            head = name,
            base = "dev",
            link = "Closes #$issue",
            draft = false,
            usage = "task final $usage",
        )
        publisher.publish(draft = draft, console = console)
        board.move(item = board.item(issue = issue, nodeId = issues.nodeId(number = issue)), status = Board.IN_REVIEW)
        console.line(text = "#$issue → ${Board.IN_REVIEW}")
    }
}

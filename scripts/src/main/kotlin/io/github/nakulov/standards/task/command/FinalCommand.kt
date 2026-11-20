package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.BranchName
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues

/**
 * `task final <N>` из `long/<N>-…` или `task/<N>-final`: итоговый PR `task/<N>-final` → `dev` с `Closes #N`, сразу к
 * ревью (части уже проверены), In review. Final — от `origin/long/<N>-…`, повтор вливает в неё свежую long; в
 * long-ветку ничего не пушится — её защищает ruleset `standards/long` (#66).
 */
class FinalCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
    private val publisher: Publisher,
) : Command {
    override val usage: String =
        "<N> --title «…» --body-file <файл> [--check «команда»]… — итоговый PR task/<N>-final → dev из long/<N>-…"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val issue = args.firstOrNull()?.toIntOrNull()
            ?: throw TaskError.Failure(message = "использование: task final $usage")
        val final = BranchName(issue = issue, slug = SLUG, long = false).toString()
        val current = git.currentBranch()
        val long = longBranch(issue = issue, current = current, final = final)
        if (!git.isClean()) {
            throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
        }
        git.fetch()
        prepare(issue = issue, final = final, long = "origin/$long", current = current)
        val draft = PullRequestDraft.of(
            args = args.drop(n = 1),
            head = final,
            base = "dev",
            link = "Closes #$issue",
            draft = false,
            usage = "task final $usage",
        )
        publisher.publish(draft = draft, console = console)
        board.move(item = board.item(issue = issue, nodeId = issues.nodeId(number = issue)), status = Board.IN_REVIEW)
        console.line(text = "#$issue → ${Board.IN_REVIEW}")
    }

    /** Long-ветка задачи: текущая или, из ветки final, найденная по номеру задачи. */
    private fun longBranch(
        issue: Int,
        current: String,
        final: String,
    ): String {
        val branch = BranchName.parse(branch = current)
        if (branch != null && branch.long && branch.issue == issue) {
            return current
        }
        if (current == final) {
            return git.longBranch(issue = issue)
                ?: throw TaskError.Refusal(message = "у задачи #$issue нет long-ветки — итоговый PR не из чего собрать")
        }
        throw TaskError.Refusal(message = "task final $issue запускается из long/$issue-… или $final, сейчас «$current»")
    }

    /** Ветка final — на свежей long: новая от неё или существующая с влитой long (повтор, #66). */
    private fun prepare(
        issue: Int,
        final: String,
        long: String,
        current: String,
    ) {
        if (!git.hasLocalBranch(name = final) && !git.hasRemoteBranch(name = final)) {
            git.createBranch(name = final, base = long)
            return
        }
        if (current != final) {
            git.switchTo(name = final)
        }
        if (!git.merge(ref = long)) {
            throw TaskError.Refusal(
                message = "слияние $long в $final не прошло — разрешите конфликт и закоммитьте или проверьте, " +
                    "что $long существует; затем повторите task final $issue",
            )
        }
    }

    companion object {
        /** Slug ветки итогового PR `task/<N>-final`; по нему `task done` узнаёт, что удалить и long (#66). */
        const val SLUG = "final"
    }
}

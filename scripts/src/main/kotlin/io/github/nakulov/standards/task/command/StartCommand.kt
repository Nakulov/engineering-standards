package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.BranchName
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.IssueInfo
import io.github.nakulov.standards.task.github.Issues

/**
 * `task start <N> <slug> [--long]`: проверка готовности (#2, `take`, пункт 1 — статус и блокеры), ветка от свежего
 * `origin/dev`, статус In progress. Исследование — без ветки, эпик в работу не берётся. Повторный запуск безопасен.
 */
class StartCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
) : Command {
    override val usage: String = "<N> <slug> [--long] — ветка task/<N>-<slug> от origin/dev, статус In progress"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val branch = parse(args = args)
        if (!git.isClean()) {
            throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
        }
        val info = issues.info(number = branch.issue)
        val item = board.item(issue = branch.issue, nodeId = issues.nodeId(number = branch.issue))
        checkReady(info = info, status = item.status)
        if (info.type == RESEARCH) {
            console.line(text = "#${branch.issue} — исследование: ветка не нужна")
        } else {
            switchToBranch(branch = branch, console = console)
        }
        if (item.status == Board.IN_PROGRESS) {
            console.line(text = "#${branch.issue} уже ${Board.IN_PROGRESS}")
        } else {
            board.move(item = item, status = Board.IN_PROGRESS)
            console.line(text = "#${branch.issue} → ${Board.IN_PROGRESS}")
        }
    }

    private fun parse(args: List<String>): BranchName {
        val long = LONG_FLAG in args
        val positional = args - LONG_FLAG
        val issue = positional.firstOrNull()?.toIntOrNull()
        if (positional.size != 2 || issue == null) {
            throw TaskError.Failure(message = "использование: task start $usage")
        }
        return BranchName.of(issue = issue, slug = positional.last(), long = long)
    }

    private fun checkReady(
        info: IssueInfo,
        status: String,
    ) {
        val reason = when {
            !info.open -> "закрыта"
            info.type == EPIC -> "— эпик: в работу берутся его подзадачи"
            status != Board.GROOMED && status != Board.IN_PROGRESS -> "в статусе «$status» — сначала груминг"
            info.openBlockers.isNotEmpty() -> "заблокирована: ${blockers(info = info)} открыты"
            else -> null
        }
        if (reason != null) {
            throw TaskError.Refusal(message = "#${info.number} $reason")
        }
    }

    private fun blockers(info: IssueInfo): String {
        return info.openBlockers.joinToString(separator = ", ") { blocker -> "#$blocker" }
    }

    private fun switchToBranch(
        branch: BranchName,
        console: Console,
    ) {
        val name = branch.toString()
        git.fetch()
        if (git.hasLocalBranch(name = name) || git.hasRemoteBranch(name = name)) {
            git.switchTo(name = name)
            console.line(text = "ветка $name уже есть — переключился на неё")
        } else {
            git.createBranch(name = name, base = BASE)
            console.line(text = "ветка $name от $BASE")
        }
    }

    private companion object {
        const val LONG_FLAG = "--long"
        const val BASE = "origin/dev"
        const val EPIC = "type:epic"
        const val RESEARCH = "type:research"
    }
}

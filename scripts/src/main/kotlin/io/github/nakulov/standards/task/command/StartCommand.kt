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
 * `task start <N> <slug> [--long | --from long/<M>-…]`: проверка готовности (#2, `take`, пункт 1 — статус и блокеры),
 * ветка от свежего `origin/dev` (или от long-ветки — для маленького PR в неё), статус In progress. Long-ветка сразу
 * отправляется в `origin`, чтобы от неё начинались части. Исследование — без ветки, эпик в работу не берётся.
 * Повторный запуск безопасен.
 */
class StartCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
) : Command {
    override val usage: String =
        "<N> <slug> [--long | --from long/<M>-…] — ветка task/<N>-<slug> от origin/dev, статус In progress"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val request = parse(args = args)
        val branch = request.branch
        if (!git.isClean()) {
            throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
        }
        val info = issues.info(number = branch.issue)
        val item = board.item(issue = branch.issue, nodeId = issues.nodeId(number = branch.issue))
        checkReady(info = info, status = item.status)
        if (info.type == RESEARCH) {
            console.line(text = "#${branch.issue} — исследование: ветка не нужна")
        } else {
            switchToBranch(request = request, console = console)
        }
        if (item.status == Board.IN_PROGRESS) {
            console.line(text = "#${branch.issue} уже ${Board.IN_PROGRESS}")
        } else {
            board.move(item = item, status = Board.IN_PROGRESS)
            console.line(text = "#${branch.issue} → ${Board.IN_PROGRESS}")
        }
    }

    private fun parse(args: List<String>): Request {
        val fromIndex = args.indexOf(FROM_FLAG)
        val from = if (fromIndex >= 0) args.getOrNull(index = fromIndex + 1) else null
        val rest = if (fromIndex >= 0) args.take(n = fromIndex) + args.drop(n = fromIndex + 2) else args
        val long = LONG_FLAG in rest
        val positional = rest - LONG_FLAG
        val issue = positional.firstOrNull()?.toIntOrNull()
        val badFrom = fromIndex >= 0 && (long || from == null || BranchName.parse(branch = from)?.long != true)
        if (positional.size != 2 || issue == null || badFrom) {
            throw TaskError.Failure(message = "использование: task start $usage")
        }
        return Request(branch = BranchName.of(issue = issue, slug = positional.last(), long = long), from = from)
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
        request: Request,
        console: Console,
    ) {
        val name = request.branch.toString()
        git.fetch()
        if (git.hasLocalBranch(name = name) || git.hasRemoteBranch(name = name)) {
            git.switchTo(name = name)
            console.line(text = "ветка $name уже есть — переключился на неё")
        } else {
            createBranch(request = request, console = console)
        }
    }

    private fun createBranch(
        request: Request,
        console: Console,
    ) {
        val name = request.branch.toString()
        val from = request.from
        if (from != null && !git.hasRemoteBranch(name = from)) {
            throw TaskError.Refusal(message = "нет ветки origin/$from — сначала task start <N> <slug> --long")
        }
        val base = "origin/${from ?: DEV}"
        git.createBranch(name = name, base = base)
        if (from != null) {
            git.setBase(branch = name, base = from)
        }
        console.line(text = "ветка $name от $base")
        if (request.branch.long) {
            git.push(name = name)
            console.line(text = "ветка $name отправлена в origin")
        }
    }

    /** Разобранные аргументы: ветка задачи и long-ветка, от которой она начинается (`--from`). */
    private data class Request(
        val branch: BranchName,
        val from: String?,
    )

    private companion object {
        const val LONG_FLAG = "--long"
        const val FROM_FLAG = "--from"
        const val DEV = "dev"
        const val EPIC = "type:epic"
        const val RESEARCH = "type:research"
    }
}

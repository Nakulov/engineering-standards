package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.IssueTemplate
import io.github.nakulov.standards.task.github.Issues
import java.io.File

/**
 * `task new <тип>`: задача с готовым телом и меткой типа из шаблона `.github/ISSUE_TEMPLATE/<тип>.md`, на доске
 * репозитория в Backlog, со связями «подзадача» и «blocked by». Что заводить, решают в разговоре — команда только
 * создаёт уже решённое.
 */
class NewCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
) : Command {
    override val usage: String = "<тип> --title «…» --body-file <файл> [--parent N] [--blocked-by N,N] — " +
        "задача по шаблону типа в Backlog"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val type = args.firstOrNull() ?: throw TaskError.Failure(message = "использование: task new $usage")
        val options = Options.parse(
            args = args.drop(n = 1),
            allowed = setOf(TITLE, BODY_FILE, PARENT, BLOCKED_BY),
            required = setOf(TITLE, BODY_FILE),
            usage = "task new $usage",
        )
        val parent = options[PARENT]?.let { value -> number(value = value) }
        val blockedBy = options[BLOCKED_BY]?.split(",")?.map { value -> number(value = value) } ?: emptyList()
        val label = IssueTemplate.typeLabel(text = readTemplate(type = type), file = "$TEMPLATES/$type.md")
        val body = Options.readFile(path = options.getValue(BODY_FILE), what = "тело задачи по шаблону")
        val issue = issues.create(
            title = options.getValue(TITLE),
            body = body,
            label = label,
            parent = parent,
            blockedBy = blockedBy,
        )
        console.line(text = "#$issue создана: $label" + (parent?.let { number -> ", подзадача #$number" } ?: ""))
        val item = board.add(
            issue = issue,
            databaseId = issues.databaseId(number = issue),
            nodeId = issues.nodeId(number = issue),
        )
        if (item.status != Board.BACKLOG) {
            board.move(item = item, status = Board.BACKLOG)
        }
        console.line(text = "#$issue → ${Board.BACKLOG}")
    }

    private fun readTemplate(type: String): String {
        val directory = File(git.root(), TEMPLATES)
        val file = File(directory, "$type.md")
        if (!file.isFile) {
            val types = directory.listFiles { candidate -> candidate.extension == "md" }.orEmpty()
                .map { candidate -> candidate.nameWithoutExtension }.sorted().joinToString(separator = ", ")
            val known = types.ifEmpty { "нет шаблонов" }
            throw TaskError.Failure(message = "нет шаблона $TEMPLATES/$type.md — типы: $known")
        }
        return file.readText()
    }

    private fun number(value: String): Int {
        return value.trim().removePrefix(prefix = "#").toIntOrNull()
            ?: throw TaskError.Failure(message = "«$value» — не номер задачи")
    }

    private companion object {
        const val TITLE = "--title"
        const val BODY_FILE = "--body-file"
        const val PARENT = "--parent"
        const val BLOCKED_BY = "--blocked-by"
        const val TEMPLATES = ".github/ISSUE_TEMPLATE"
    }
}

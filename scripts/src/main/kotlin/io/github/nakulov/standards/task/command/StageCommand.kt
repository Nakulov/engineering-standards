package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import io.github.nakulov.standards.task.github.StageBody
import java.io.File

/**
 * `task stage create`: веха с описанием по шаблону этапа [template] (`templates/github/stage.md` плагина); при пустом
 * разделе веха не создаётся (#2, «`ROADMAP.md`», пункт 7).
 */
class StageCommand(
    private val gitHub: GitHub,
    private val template: File,
) : Command {
    override val usage: String = "create --title «…» --body-file <файл> [--due ГГГГ-ММ-ДД] — веха этапа по шаблону"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        if (args.firstOrNull() != CREATE) {
            throw TaskError.Failure(message = "использование: task stage $usage")
        }
        val options = Options.parse(
            args = args.drop(n = 1),
            allowed = setOf(TITLE, BODY_FILE, DUE),
            required = setOf(TITLE, BODY_FILE),
            usage = "task stage $usage",
        )
        val due = options[DUE]
        if (due != null && !DATE.matches(input = due)) {
            throw TaskError.Failure(message = "срок «$due» — в формате ГГГГ-ММ-ДД")
        }
        val text = Options.readFile(path = options.getValue(BODY_FILE), what = "описание этапа по шаблону")
        val empty = StageBody.emptySections(template = template.readText(), text = text)
        if (empty.isNotEmpty()) {
            val names = empty.joinToString(separator = ", ") { section -> "«$section»" }
            throw TaskError.Refusal(message = "в описании этапа пусты разделы: $names — заполните, пустой — «Нет»")
        }
        val title = options.getValue(TITLE)
        val fields = listOf("-f", "title=$title", "-f", "description=${text.trim()}") +
            (due?.let { date -> listOf("-f", "due_on=${date}T00:00:00Z") } ?: emptyList())
        val create = listOf("api", "--method", "POST", "repos/{owner}/{repo}/milestones")
        val url = gitHub.write(args = create + fields + listOf("--jq", ".html_url"))
        console.line(text = "этап «$title»: $url")
    }

    private companion object {
        const val CREATE = "create"
        const val TITLE = "--title"
        const val BODY_FILE = "--body-file"
        const val DUE = "--due"
        val DATE = Regex(pattern = "[0-9]{4}-[0-9]{2}-[0-9]{2}")
    }
}

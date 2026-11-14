package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import java.net.URLEncoder

/**
 * Метки (#8, решение 8): создаёт и сверяет метки типа из `templates/github/labels.yml` — на них держатся `task new` и
 * `task start`. Стандартные метки GitHub удаляет: без задач — сразу, с задачами — только с `--confirm`. Остальные
 * метки не угадывает и не переносит — только называет: какая метка проекта значит эпик, по имени не узнать.
 */
class Labels(
    private val gitHub: GitHub,
    private val standard: List<Label>,
) : Setup.Step {
    /** Метка: [color] — шесть цифр hex без `#`. */
    data class Label(
        val name: String,
        val color: String,
        val description: String,
    )

    override fun run(mode: Setup.Mode): List<Finding> {
        val actual = gitHub.listRest(path = "$REPOSITORY/labels", jq = ROW_JQ).associate { row ->
            val (name, color, description) = "$row\t\t".split("\t")
            name to Label(name = name, color = color, description = description)
        }
        val typed = standard.mapNotNull { label -> syncType(label = label, actual = actual[label.name], mode = mode) }
        val defaults = actual.keys.filter { name -> name in GITHUB_DEFAULTS }.map { name ->
            removeDefault(name = name, mode = mode)
        }
        val known = standard.map { label -> label.name }.toSet() + GITHUB_DEFAULTS
        val foreign = actual.keys.filter { name -> name !in known }.sorted()
        val note = if (foreign.isEmpty()) {
            emptyList()
        } else {
            val text = "вне стандарта: ${foreign.joinToString(separator = ", ")} — тип задачи только type:*, " +
                "перенос старой метки — вручную: gh label edit <старая> --name type:…"
            listOf(Finding(status = Finding.Status.NOTE, area = AREA, text = text))
        }
        val ok = if (typed.isEmpty()) {
            listOf(Finding(status = Finding.Status.OK, area = AREA, text = "метки типа соответствуют стандарту"))
        } else {
            emptyList()
        }
        return ok + typed + defaults + note
    }

    private fun syncType(
        label: Label,
        actual: Label?,
        mode: Setup.Mode,
    ): Finding? {
        val same = actual != null && actual.color.equals(label.color, ignoreCase = true) &&
            actual.description == label.description
        if (same) {
            return null
        }
        val fields = listOf("-f", "color=${label.color}", "-f", "description=${label.description}")
        if (mode.apply && actual == null) {
            val name = listOf("-f", "name=${label.name}")
            gitHub.write(args = listOf("api", "--method", "POST", "$REPOSITORY/labels") + name + fields)
        } else if (mode.apply) {
            val path = "$REPOSITORY/labels/${encode(name = label.name)}"
            gitHub.write(args = listOf("api", "--method", "PATCH", path) + fields)
        }
        val status = if (mode.apply) Finding.Status.FIXED else Finding.Status.DEVIATION
        val text = if (actual == null) "нет метки ${label.name}" else "${label.name} отличается цветом или описанием"
        return Finding(status = status, area = AREA, text = text)
    }

    private fun removeDefault(
        name: String,
        mode: Setup.Mode,
    ): Finding {
        val path = "$REPOSITORY/issues?state=all&per_page=1&labels=${encode(name = name)}"
        val used = gitHub.read(args = listOf("api", path, "--jq", "length")) != "0"
        val status = when {
            !mode.apply -> Finding.Status.DEVIATION
            used && !mode.confirmed -> Finding.Status.NEEDS_CONFIRM
            else -> Finding.Status.FIXED
        }
        if (status == Finding.Status.FIXED) {
            gitHub.write(args = listOf("api", "--method", "DELETE", "$REPOSITORY/labels/${encode(name = name)}"))
        }
        val where = if (used) "стоит на задачах" else "не используется"
        val text = when (status) {
            Finding.Status.FIXED -> "стандартная метка GitHub $name удалена ($where)"
            Finding.Status.NEEDS_CONFIRM -> "удалит стандартную метку GitHub $name ($where)"
            else -> "стандартная метка GitHub $name ($where) — удаляется"
        }
        return Finding(status = status, area = AREA, text = text)
    }

    private fun encode(name: String): String {
        return URLEncoder.encode(name, Charsets.UTF_8).replace(oldValue = "+", newValue = "%20")
    }

    companion object {
        private const val AREA = "метки"
        private const val REPOSITORY = "repos/{owner}/{repo}"
        private const val ROW_JQ = ".[] | \"\\(.name)\\t\\(.color)\\t\\(.description // \"\")\""
        private const val ITEM = "- "
        private const val SEPARATOR = ": "
        private const val QUOTE = "\""

        /** Метки, которые GitHub создаёт в каждом новом репозитории. */
        val GITHUB_DEFAULTS = setOf(
            "bug",
            "documentation",
            "duplicate",
            "enhancement",
            "good first issue",
            "help wanted",
            "invalid",
            "question",
            "wontfix",
            "accessibility",
        )

        /**
         * Метки из `labels.yml`: элемент начинается с `- name: …`, следом строки `  ключ: значение`; `#` — комментарий.
         * Формат читается без библиотеки YAML — у скриптов нет зависимостей, — но остаётся валидным YAML: значение
         * с `: ` или `#` — в двойных кавычках, без экранирования внутри; без кавычек такое значение — ошибка.
         */
        fun parse(
            text: String,
            file: String,
        ): List<Label> {
            val items = mutableListOf<MutableMap<String, String>>()
            val lines = text.lines().filter { line -> line.isNotBlank() && !line.trimStart().startsWith(prefix = "#") }
            lines.forEach { line ->
                if (line.startsWith(prefix = ITEM)) {
                    items += mutableMapOf()
                }
                val pair = line.removePrefix(prefix = ITEM).trim()
                val current = items.lastOrNull()
                if (current == null || SEPARATOR !in pair) {
                    throw TaskError.Failure(message = "$file: «$line» — ожидается «- name: …» или «  ключ: значение»")
                }
                current[pair.substringBefore(delimiter = SEPARATOR)] = value(
                    raw = pair.substringAfter(delimiter = SEPARATOR),
                    line = line,
                    file = file,
                )
            }
            return items.map { item ->
                val name = item["name"]
                val color = item["color"]
                val description = item["description"]
                if (name == null || color == null || description == null) {
                    val label = name ?: "без имени"
                    throw TaskError.Failure(message = "$file: у метки $label нужны name, color и description")
                }
                Label(name = name, color = color, description = description)
            }
        }

        private fun value(
            raw: String,
            line: String,
            file: String,
        ): String {
            val quoted = raw.length >= 2 && raw.startsWith(prefix = QUOTE) && raw.endsWith(suffix = QUOTE)
            val inner = if (quoted) raw.substring(startIndex = 1, endIndex = raw.length - 1) else raw
            val broken = if (quoted) QUOTE in inner || "\\" in inner else SEPARATOR in raw || " #" in raw
            if (broken) {
                throw TaskError.Failure(message = "$file: «$line» — значение с «: » или «#» — в кавычках, без \" и \\")
            }
            return inner
        }
    }
}

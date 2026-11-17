package io.github.nakulov.standards.task.github

import io.github.nakulov.standards.task.cli.TaskError

/**
 * Части плана в теле задачи (#53): каждая — между метками `<!-- план -->` … `<!-- /план -->` в своём разделе.
 * Повтор заменяет содержимое меток; строки вне меток (груминг, ручные правки) не трогаются. Тело из веб-интерфейса
 * GitHub приходит с `\r\n` — переводы строк приводятся к `\n`, иначе метки не найдутся и блок задвоится.
 */
object IssueParts {
    private const val OPEN = "<!-- план -->"
    private const val CLOSE = "<!-- /план -->"
    private const val HEADING = "## "
    private const val BODY_START = "начало тела"
    private val BOLD = Regex(pattern = "^\\*\\*[^*]+:\\*\\*$")

    fun merge(
        body: String,
        parts: String,
        planLine: String,
    ): String {
        var lines = body.replace(oldValue = "\r\n", newValue = "\n").lines()
        val first = lines.indexOfFirst { line -> line.startsWith(prefix = HEADING) }
            .takeIf { index -> index >= 0 } ?: lines.size
        lines = place(lines = lines, start = 0, end = first, content = listOf(planLine), name = BODY_START)
        sections(text = parts).forEach { (name, content) ->
            val heading = lines.indexOfFirst { line -> line.trim() == "$HEADING$name" || line.trim() == "**$name:**" }
            if (heading < 0) {
                throw TaskError.Refusal(message = "в задаче нет раздела «$name» — исправьте заголовок в файле частей")
            }
            val end = (heading + 1 until lines.size).firstOrNull { index -> isHeading(line = lines[index]) }
                ?: lines.size
            lines = place(lines = lines, start = heading + 1, end = end, content = content, name = name)
        }
        return lines.joinToString(separator = "\n")
    }

    private fun place(
        lines: List<String>,
        start: Int,
        end: Int,
        content: List<String>,
        name: String,
    ): List<String> {
        val span = lines.subList(fromIndex = start, toIndex = end)
        val opens = span.count { line -> line == OPEN }
        val closes = span.count { line -> line == CLOSE }
        val open = span.indexOf(element = OPEN)
        val close = span.indexOf(element = CLOSE)
        if (opens != closes || opens > 1 || close < open) {
            throw TaskError.Refusal(message = "в разделе «$name» метка $OPEN без пары — поправьте тело задачи")
        }
        val block = listOf(OPEN) + content + CLOSE
        if (opens == 1) {
            return lines.take(n = start + open) + block + lines.drop(n = start + close + 1)
        }
        val last = span.indexOfLast { line -> line.isNotBlank() }
        val at = start + last + 1
        val gap = if (last >= 0) listOf("") else emptyList()
        return lines.take(n = at) + gap + block + lines.drop(n = at)
    }

    private fun isHeading(line: String): Boolean {
        return line.startsWith(prefix = HEADING) || BOLD.matches(input = line.trim())
    }

    /** Разделы файла частей: `## <название>` → строки без пустых по краям. */
    private fun sections(text: String): List<Pair<String, List<String>>> {
        val result = mutableListOf<Pair<String, MutableList<String>>>()
        text.lines().forEach { line ->
            if (line.startsWith(prefix = HEADING)) {
                result += line.removePrefix(prefix = HEADING).trim() to mutableListOf()
            } else {
                result.lastOrNull()?.second?.add(line)
            }
        }
        return result.map { (name, content) ->
            name to content.dropWhile { line -> line.isBlank() }.dropLastWhile { line -> line.isBlank() }
        }
    }
}

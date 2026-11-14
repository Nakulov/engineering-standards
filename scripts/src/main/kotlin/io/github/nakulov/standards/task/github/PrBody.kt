package io.github.nakulov.standards.task.github

import io.github.nakulov.standards.task.cli.TaskError

/**
 * Тело PR из текста ассистента по шаблону PR: связь с задачей (`Closes #N` или `Part of #N`) — на месте заготовки
 * `Closes #` шаблона, без неё — первой строкой; строка «**Автоматически:**» — пройденные проверки (#2, «Шаблон PR»,
 * пункты 3 и 6). Комментарии шаблона, занимающие строку целиком, вне блоков кода вырезаются: это подсказки для
 * написания, а тело PR — тело squash-коммита (#41). Остальное — как написал ассистент.
 */
object PrBody {
    private const val AUTOMATIC = "**Автоматически:**"
    private const val OPEN = "<!--"
    private const val CLOSE = "-->"
    private const val FENCE = "```"
    private val LINK = Regex(pattern = "^(Closes|Part of) #[0-9]*$")

    fun compose(
        text: String,
        link: String,
        checks: List<String>,
    ): String {
        val lines = withLink(lines = withoutComments(lines = text.trim().lines()), link = link)
        val automatic = lines.indexOfFirst { line -> line.startsWith(prefix = AUTOMATIC) }
        if (automatic < 0) {
            throw TaskError.Failure(message = "в тексте PR нет строки «$AUTOMATIC» — пишите тело по шаблону PR")
        }
        val passed = checks.map { check -> "- `$check` — прошла" }
        val results = if (checks.isEmpty()) listOf("$AUTOMATIC нет") else listOf(AUTOMATIC) + passed
        val body = lines.take(n = automatic) + results + lines.drop(n = automatic + 1)
        return body.joinToString(separator = "\n") + "\n"
    }

    /**
     * Строки без комментариев, которые занимают строку целиком (и многострочных); `<!-- … -->` внутри текста, кода
     * или блока ``` остаётся. Пустая строка после вырезанного комментария не удваивает пустую перед ним.
     */
    private fun withoutComments(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var fence = false
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (line.trimStart().startsWith(prefix = FENCE)) {
                fence = !fence
            }
            val opens = !fence && line.trimStart().startsWith(prefix = OPEN)
            val end = if (opens) commentEnd(lines = lines, start = index) else -1
            if (end < 0) {
                result += line
                index += 1
                continue
            }
            val blankBefore = result.isEmpty() || result.last().isBlank()
            val blankAround = blankBefore && lines.getOrNull(index = end + 1)?.isBlank() == true
            index = if (blankAround) end + 2 else end + 1
        }
        return result
    }

    /** Индекс строки, где кончается комментарий с [start], или -1, если после `-->` в ней есть текст. */
    private fun commentEnd(
        lines: List<String>,
        start: Int,
    ): Int {
        val end = (start until lines.size).firstOrNull { index -> CLOSE in lines[index] } ?: return -1
        val line = lines[end].trimEnd()
        return if (line.indexOf(string = CLOSE) == line.length - CLOSE.length) end else -1
    }

    private fun withLink(
        lines: List<String>,
        link: String,
    ): List<String> {
        val index = lines.indexOfFirst { line -> LINK.matches(input = line) }
        return if (index < 0) listOf(link) + lines else lines.take(n = index) + link + lines.drop(n = index + 1)
    }
}

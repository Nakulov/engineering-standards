package io.github.nakulov.standards.task.github

import io.github.nakulov.standards.task.cli.TaskError

/**
 * Тело PR из текста ассистента по шаблону PR: связь с задачей (`Closes #N` или `Part of #N`) — на месте заготовки
 * `Closes #` шаблона, без неё — первой строкой; строка «**Автоматически:**» — пройденные проверки (#2, «Шаблон PR»,
 * пункты 3 и 6). Остальное — как написал ассистент.
 */
object PrBody {
    private const val AUTOMATIC = "**Автоматически:**"
    private val LINK = Regex(pattern = "^(Closes|Part of) #[0-9]*$")

    fun compose(
        text: String,
        link: String,
        checks: List<String>,
    ): String {
        val lines = withLink(lines = text.trim().lines(), link = link)
        val automatic = lines.indexOfFirst { line -> line.startsWith(prefix = AUTOMATIC) }
        if (automatic < 0) {
            throw TaskError.Failure(message = "в тексте PR нет строки «$AUTOMATIC» — пишите тело по шаблону PR")
        }
        val passed = checks.map { check -> "- `$check` — прошла" }
        val results = if (checks.isEmpty()) listOf("$AUTOMATIC нет") else listOf(AUTOMATIC) + passed
        val body = lines.take(n = automatic) + results + lines.drop(n = automatic + 1)
        return body.joinToString(separator = "\n") + "\n"
    }

    private fun withLink(
        lines: List<String>,
        link: String,
    ): List<String> {
        val index = lines.indexOfFirst { line -> LINK.matches(input = line) }
        return if (index < 0) listOf(link) + lines else lines.take(n = index) + link + lines.drop(n = index + 1)
    }
}

package io.github.nakulov.standards.task.github

import io.github.nakulov.standards.task.cli.TaskError

/**
 * Метка типа из заголовка шаблона задачи: `labels: [type:task]` между строками `---` (#6, решение 1).
 */
object IssueTemplate {
    private const val FENCE = "---"
    private val LABELS = Regex(pattern = "^labels:\\s*\\[(.*)]\\s*$")

    fun typeLabel(
        text: String,
        file: String,
    ): String {
        val header = text.lines().drop(n = 1).takeWhile { line -> line.trim() != FENCE }
        val match = header.firstNotNullOfOrNull { line -> LABELS.matchEntire(input = line.trim()) }
        val labels = match?.groupValues?.get(1)?.split(",")?.map { label -> label.trim() }.orEmpty()
        val type = labels.firstOrNull { label -> label.startsWith(prefix = "type:") }
        if (!text.startsWith(prefix = FENCE) || type == null) {
            throw TaskError.Failure(message = "в заголовке $file нет метки типа — нужна строка labels: [type:…]")
        }
        return type
    }
}

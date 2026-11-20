package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.TaskError

/**
 * Сообщение `task commit` по шаблону задачи плана (#53): заголовок, «Задача плана: …», «Файлы:» — пути для коммита.
 * «Задача плана» обязательна, только если в ветке есть план (#82): задача «сразу в работу» коммитит без неё.
 */
data class CommitMessage(
    val title: String,
    val planTask: String?,
    val files: List<String>,
) {
    companion object {
        private const val PLAN_TASK = "Задача плана:"
        private const val FILES = "Файлы:"
        private val FILE_LINE = Regex(pattern = "^- `([^`]+)` — .+$")

        /** [plan] — план ветки `plans/<slug>.md`; `null` — плана нет, строка «Задача плана» не нужна. */
        fun parse(
            text: String,
            plan: String?,
        ): CommitMessage {
            val lines = text.lines()
            val title = lines.first().trim()
            if (title.isEmpty()) {
                throw TaskError.Refusal(message = "первая строка сообщения пуста — напишите, что сделано")
            }
            val planTask = lines.firstOrNull { line -> line.startsWith(prefix = PLAN_TASK) }
                ?.removePrefix(prefix = PLAN_TASK)
                ?.trim()
                ?.takeIf { task -> task.isNotEmpty() }
            if (planTask == null && plan != null) {
                throw TaskError.Refusal(
                    message = "в ветке план `$plan` — нет строки «Задача плана: …», добавьте заголовок задачи плана",
                )
            }
            val files = lines.dropWhile { line -> line.trim() != FILES }
                .drop(n = 1)
                .mapNotNull { line -> FILE_LINE.matchEntire(input = line.trim())?.groupValues?.get(index = 1) }
            if (files.isEmpty()) {
                throw TaskError.Refusal(
                    message = "раздел «Файлы:» пуст — перечислите файлы строками «- `путь` — что изменено»",
                )
            }
            return CommitMessage(title = title, planTask = planTask, files = files)
        }
    }
}

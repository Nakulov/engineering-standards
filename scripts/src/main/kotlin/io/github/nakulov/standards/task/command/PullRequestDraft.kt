package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.TaskError
import java.io.File

/**
 * Что публикуется в PR: ветка и база, связь с задачей, заголовок и текст ассистента, проверки, черновик ли.
 */
data class PullRequestDraft(
    val head: String,
    val base: String,
    val link: String,
    val title: String,
    val text: String,
    val checks: List<String>,
    val draft: Boolean,
) {
    companion object {
        private const val TITLE = "--title"
        private const val BODY_FILE = "--body-file"
        private const val CHECK = "--check"

        /** Разбор `--title`, `--body-file`, `--check …` (повторяется); остальное — ошибка с [usage]. */
        fun of(
            args: List<String>,
            head: String,
            base: String,
            link: String,
            draft: Boolean,
            usage: String,
        ): PullRequestDraft {
            val pairs = args.chunked(size = 2)
            val known = pairs.all { pair -> pair.size == 2 && pair.first() in listOf(TITLE, BODY_FILE, CHECK) }
            val title = pairs.lastOrNull { pair -> pair.first() == TITLE }?.last()
            val bodyFile = pairs.lastOrNull { pair -> pair.first() == BODY_FILE }?.last()
            if (!known || title.isNullOrBlank() || bodyFile == null) {
                throw TaskError.Failure(message = "использование: $usage")
            }
            val file = File(bodyFile)
            if (!file.isFile) {
                throw TaskError.Failure(message = "нет файла $bodyFile — сначала запишите текст PR по шаблону")
            }
            return PullRequestDraft(
                head = head,
                base = base,
                link = link,
                title = title,
                text = file.readText(),
                checks = pairs.filter { pair -> pair.first() == CHECK }.map { pair -> pair.last() },
                draft = draft,
            )
        }
    }
}

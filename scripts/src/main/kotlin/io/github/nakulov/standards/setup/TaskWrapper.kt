package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError
import java.io.File
import java.io.IOException

/**
 * Обёртка `~/.local/bin/task` для терминала (#43, решение 6): одна на машину, единственная запись `setup` вне
 * репозитория. Свою обёртку узнаёт по метке и обновляет без `--confirm`; чужой `task` (например go-task) не трогает.
 * `--check` без обёртки — заметка: на CI и чужих машинах её нет, это не отклонение репозитория.
 */
class TaskWrapper(
    private val home: File,
    private val pluginRoot: File,
    private val path: String,
) : Setup.Step {
    override fun run(mode: Setup.Mode): List<Finding> {
        val source = File(pluginRoot, SOURCE).readText()
        val target = File(home, TARGET)
        val current = if (target.isFile) target.readText() else null
        val wrapper = when {
            current == source -> finding(status = Finding.Status.OK, text = "обёртка по стандарту")
            current != null && MARKER !in current -> finding(
                status = Finding.Status.NOTE,
                text = "чужая программа, не трогаю — task из терминала: ${File(pluginRoot, "scripts/task").path}",
            )
            !mode.apply && current == null ->
                finding(status = Finding.Status.NOTE, text = "обёртки нет — task из терминала не найдётся, поставит setup")
            !mode.apply -> finding(status = Finding.Status.DEVIATION, text = "обёртка отличается от стандарта — обновит setup")
            else -> {
                install(target = target, source = source)
                val action = if (current == null) "установлена" else "обновлена"
                finding(status = Finding.Status.FIXED, text = "обёртка $action — запись вне репозитория, одна на машину")
            }
        }
        val bin = File(home, BIN).path
        if (path.split(':').any { entry -> entry.trimEnd('/') == bin }) {
            return listOf(wrapper)
        }
        val note = "~/$BIN нет в PATH — добавьте в ~/.zshrc: export PATH=\"\$HOME/$BIN:\$PATH\""
        return listOf(wrapper, Finding(status = Finding.Status.NOTE, area = AREA, text = note))
    }

    private fun install(
        target: File,
        source: String,
    ) {
        try {
            target.parentFile.mkdirs()
            target.writeText(source)
        } catch (@Suppress("SwallowedException") error: IOException) {
            throw TaskError.Failure(message = "~/$TARGET: не записана — проверьте права на ~/$BIN")
        }
        // Ветка только кодом, без теста: снять право на chmod у владельца файла в тесте нельзя.
        if (!target.setExecutable(true)) {
            throw TaskError.Failure(message = "~/$TARGET: не выставлены права на запуск — chmod +x ~/$TARGET")
        }
    }

    private fun finding(
        status: Finding.Status,
        text: String,
    ): Finding {
        return Finding(status = status, area = AREA, text = "~/$TARGET: $text")
    }

    private companion object {
        const val AREA = "машина"
        const val BIN = ".local/bin"
        const val TARGET = ".local/bin/task"
        const val SOURCE = "scripts/task-wrapper"
        const val MARKER = "# engineering-standards task-wrapper"
    }
}

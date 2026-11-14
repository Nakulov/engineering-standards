package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.Shell

/**
 * Запуск проверки перед PR (`sh -c`). Не прошла — PR не открывается: отказ с последней строкой вывода.
 */
class CheckRunner(
    private val shell: Shell,
) {
    fun run(command: String) {
        val result = shell.run(command = listOf("sh", "-c", command))
        if (result.code != 0) {
            val tail = (result.out + result.err).lines().lastOrNull { line -> line.isNotBlank() }.orEmpty()
            throw TaskError.Refusal(message = "проверка «$command» не прошла — исправьте: $tail")
        }
    }
}

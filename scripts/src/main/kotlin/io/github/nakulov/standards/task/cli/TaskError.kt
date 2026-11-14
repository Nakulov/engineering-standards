package io.github.nakulov.standards.task.cli

/**
 * Ожидаемая ошибка команды: сообщение — одна строка «что не так — что делать», код выхода — для оркестратора.
 */
sealed class TaskError(
    message: String,
    val exitCode: Int,
) : Exception(message) {
    /** Процесс не пускает: задача не Groomed, открыт блокер, грязное дерево. */
    class Refusal(
        message: String,
    ) : TaskError(message = message, exitCode = 1)

    /** Сбой: сеть, `gh`, неверные аргументы. */
    class Failure(
        message: String,
    ) : TaskError(message = message, exitCode = 2)
}

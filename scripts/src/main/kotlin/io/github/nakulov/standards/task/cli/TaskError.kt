package io.github.nakulov.standards.task.cli

/**
 * Ожидаемая ошибка команды: сообщение — одна строка «что не так — что делать», код выхода — для оркестратора.
 */
sealed class TaskError(
    message: String,
    val exitCode: Int,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** Процесс не пускает: задача не Groomed, открыт блокер, грязное дерево. */
    class Refusal(
        message: String,
    ) : TaskError(message = message, exitCode = 1)

    /** Сбой: сеть, `gh`, неверные аргументы. [cause] — исходная ошибка, видна в стектрейсе при `TASK_DEBUG=1`. */
    class Failure(
        message: String,
        cause: Throwable? = null,
    ) : TaskError(message = message, exitCode = 2, cause = cause)
}

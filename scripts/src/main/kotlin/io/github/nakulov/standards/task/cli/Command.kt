package io.github.nakulov.standards.task.cli

/**
 * Подкоманда `task`. Ожидаемый отказ или сбой бросает [TaskError].
 */
interface Command {
    /** Строка для `task --help`: аргументы и что делает команда. */
    val usage: String

    fun run(
        args: List<String>,
        console: Console,
    )
}

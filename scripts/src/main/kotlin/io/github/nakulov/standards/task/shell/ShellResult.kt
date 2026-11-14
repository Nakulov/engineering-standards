package io.github.nakulov.standards.task.shell

/**
 * Результат внешней команды: код выхода, stdout, stderr.
 */
data class ShellResult(
    val code: Int,
    val out: String,
    val err: String,
)

package io.github.nakulov.standards.task.cli

import kotlin.system.exitProcess

/**
 * Точка входа `task`: её запускает `scripts/task` из jar в кеше.
 */
fun main(args: Array<String>) {
    val task = Task(
        commands = emptyMap(),
        console = SystemConsole,
        debug = System.getenv("TASK_DEBUG") == "1",
    )
    exitProcess(task.run(args = args.toList()))
}

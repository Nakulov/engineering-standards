package io.github.nakulov.standards.task.cli

/**
 * Разбор аргументов `task`: справка, выбор подкоманды, ошибки — в код выхода (0 — сделано, 1 — отказ, 2 — сбой).
 * Стектрейс печатается только при [debug] (`TASK_DEBUG=1`).
 */
class Task(
    private val commands: Map<String, Command>,
    private val console: Console,
    private val debug: Boolean,
) {
    fun run(args: List<String>): Int {
        val code = try {
            execute(args = args)
            0
        } catch (error: TaskError) {
            report(message = error.message.orEmpty(), error = error)
            error.exitCode
        } catch (@Suppress("TooGenericExceptionCaught") error: RuntimeException) {
            report(message = "сбой скрипта: $error — подробности: TASK_DEBUG=1", error = error)
            2
        }
        return code
    }

    private fun execute(args: List<String>) {
        val name = args.firstOrNull() ?: HELP
        if (name == HELP) {
            usage().forEach { line -> console.line(text = line) }
            return
        }
        val command = commands[name] ?: throw TaskError.Failure(message = "неизвестная команда $name — см. task --help")
        command.run(args = args.drop(n = 1), console = console)
    }

    private fun usage(): List<String> {
        val lines = commands.map { (name, command) -> "  task $name ${command.usage}" }
        return listOf(TITLE, "") + lines.ifEmpty { listOf(NO_COMMANDS) }
    }

    private fun report(
        message: String,
        error: Exception,
    ) {
        console.error(text = message)
        if (debug) {
            console.error(text = error.stackTraceToString())
        }
    }

    private companion object {
        const val HELP = "--help"
        const val TITLE = "task — механика процесса: ветки, PR, доска. Команды:"
        const val NO_COMMANDS = "  (команды появятся в следующих PR задачи #13)"
    }
}

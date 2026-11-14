package io.github.nakulov.standards.task.shell

import io.github.nakulov.standards.task.cli.TaskError
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture

/**
 * Запуск команды процессом в [directory]. Нет программы — [TaskError.Failure] с подсказкой.
 */
class ProcessShell(
    private val directory: File,
) : Shell {
    override fun run(command: List<String>): ShellResult {
        val program = command.first()
        val process = try {
            ProcessBuilder(command).directory(directory).start()
        } catch (error: IOException) {
            throw TaskError.Failure(message = "не найдена команда $program — установите: brew install $program")
        }
        // stderr читается параллельно: большой вывод в один поток не заблокирует процесс.
        val err = CompletableFuture.supplyAsync { process.errorStream.bufferedReader().readText() }
        val out = process.inputStream.bufferedReader().readText()
        return ShellResult(code = process.waitFor(), out = out, err = err.get())
    }
}

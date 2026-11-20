package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import java.io.File

/**
 * `task commit --message-file <файл>` (#53): коммит по шаблону задачи плана. Пути — из раздела «Файлы:» сообщения;
 * изменённый план `plans/<имя>.md` добавляется сам — отметки `[x]` идут в коммит задачи.
 */
class CommitCommand(
    private val git: Git,
    private val directory: File,
) : Command {
    override val usage: String = "--message-file <файл> — коммит по шаблону задачи плана: файлы — из раздела «Файлы:»"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        if (args.size != 2 || args.first() != MESSAGE_FILE) {
            throw TaskError.Failure(message = "использование: task commit $usage")
        }
        val file = File(args.last()).let { path -> if (path.isAbsolute) path else File(directory, args.last()) }
        if (!file.isFile) {
            throw TaskError.Failure(message = "нет файла сообщения ${args.last()}")
        }
        val message = CommitMessage.parse(text = file.readText(), plan = null)
        val changed = git.changedPaths()
        val unchanged = message.files.firstOrNull { path -> path !in changed }
        if (unchanged != null) {
            throw TaskError.Refusal(message = "`$unchanged` не изменён или не существует — уберите из «Файлов»")
        }
        val plans = changed.filter { path -> isPlan(path = path) && path !in message.files }
        git.commitPaths(paths = message.files + plans, messageFile = file.path)
        console.line(text = "коммит: ${message.title}")
    }

    private fun isPlan(path: String): Boolean {
        return path.startsWith(prefix = "plans/") &&
            path.endsWith(suffix = ".md") &&
            path.count { char -> char == '/' } == 1
    }

    private companion object {
        const val MESSAGE_FILE = "--message-file"
    }
}

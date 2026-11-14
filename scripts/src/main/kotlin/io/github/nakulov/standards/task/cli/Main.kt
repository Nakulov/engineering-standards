package io.github.nakulov.standards.task.cli

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.command.DoneCommand
import io.github.nakulov.standards.task.command.StartCommand
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.GitHub
import io.github.nakulov.standards.task.github.Issues
import io.github.nakulov.standards.task.shell.ProcessShell
import java.io.File
import kotlin.system.exitProcess

/**
 * Точка входа `task`: её запускает `scripts/task` из jar в кеше. Команды работают в текущей папке.
 */
fun main(args: Array<String>) {
    val shell = ProcessShell(directory = File(System.getProperty("user.dir")))
    val gitHub = GitHub(shell = shell, pause = { millis -> Thread.sleep(millis) })
    val git = Git(shell = shell)
    val issues = Issues(gitHub = gitHub)
    val board = Board(gitHub = gitHub)
    val task = Task(
        commands = mapOf(
            "start" to StartCommand(git = git, issues = issues, board = board),
            "done" to DoneCommand(git = git, issues = issues, board = board),
        ),
        console = SystemConsole,
        debug = System.getenv("TASK_DEBUG") == "1",
    )
    exitProcess(task.run(args = args.toList()))
}

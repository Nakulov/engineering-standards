package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.Issues

/**
 * `task groomed <N>` (#69): груминг закончен — задача из Backlog в Groomed, после чего её берёт `task start`. Задача
 * уже в Groomed или дальше — статус не меняется; закрытая задача и эпик — отказ.
 */
class GroomedCommand(
    private val issues: Issues,
    board: Board,
) : Command {
    private val grooming = Grooming(issues = issues, board = board)

    override val usage: String = "<N> — задача из Backlog в Groomed: груминг закончен"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val number = args.singleOrNull()?.toIntOrNull()
            ?: throw TaskError.Failure(message = "использование: task groomed $usage")
        val info = issues.info(number = number)
        if (!info.open) {
            throw TaskError.Refusal(message = "#$number закрыта")
        }
        if (info.type == EPIC) {
            throw TaskError.Refusal(message = "#$number — эпик: в Groomed переводятся его подзадачи")
        }
        console.line(text = grooming.groom(number = number))
    }

    private companion object {
        const val EPIC = "type:epic"
    }
}

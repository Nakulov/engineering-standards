package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.github.Issues

/**
 * Груминг закончен (#69): задача из Backlog — в Groomed; другой статус не меняется. Общее правило `task groomed` и
 * `task plan`; отказы по закрытой задаче и эпику — у вызывающей команды.
 */
class Grooming(
    private val issues: Issues,
    private val board: Board,
) {
    /** Строка для консоли: переход или текущий статус. */
    fun groom(number: Int): String {
        val item = board.item(issue = number, nodeId = issues.nodeId(number = number))
        if (item.status != Board.BACKLOG) {
            return "#$number уже в «${item.status}»"
        }
        board.move(item = item, status = Board.GROOMED)
        return "#$number → ${Board.GROOMED}"
    }
}

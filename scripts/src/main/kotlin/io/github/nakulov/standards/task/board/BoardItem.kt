package io.github.nakulov.standards.task.board

/**
 * Задача на доске: элемент, доска, поле Status, текущий статус и варианты статуса по именам (имя → id).
 */
data class BoardItem(
    val id: String,
    val projectId: String,
    val fieldId: String,
    val status: String,
    val options: Map<String, String>,
)

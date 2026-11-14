package io.github.nakulov.standards.task.github

/**
 * Что команды знают о задаче: открыта ли, метка типа (`type:task`, `type:epic`, …), открытые «blocked by».
 */
data class IssueInfo(
    val number: Int,
    val open: Boolean,
    val type: String?,
    val openBlockers: List<Int>,
)

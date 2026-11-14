package io.github.nakulov.standards.task.github

/**
 * PR ветки: номер и слит ли он.
 */
data class PullRequest(
    val number: Int,
    val merged: Boolean,
)

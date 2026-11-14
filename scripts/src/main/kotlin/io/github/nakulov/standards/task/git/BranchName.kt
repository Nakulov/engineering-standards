package io.github.nakulov.standards.task.git

import io.github.nakulov.standards.task.cli.TaskError

/**
 * Ветка задачи: `task/<N>-<slug>` или `long/<N>-<slug>` (#2, часть «Ветки, защита и CI», пункт 8).
 * Slug даёт ассистент: заголовок задачи русский, ветка — английская (#13, решение 4).
 */
data class BranchName(
    val issue: Int,
    val slug: String,
    val long: Boolean,
) {
    override fun toString(): String {
        val kind = if (long) LONG else TASK
        return "$kind/$issue-$slug"
    }

    companion object {
        private const val TASK = "task"
        private const val LONG = "long"
        private const val MAX_SLUG = 40
        private val SLUG = Regex(pattern = "[a-z0-9]+(-[a-z0-9]+)*")
        private val BRANCH = Regex(pattern = "($TASK|$LONG)/([0-9]+)-(.+)")

        fun of(
            issue: Int,
            slug: String,
            long: Boolean,
        ): BranchName {
            if (!SLUG.matches(input = slug) || slug.length > MAX_SLUG) {
                val rule = "латиница в нижнем регистре, цифры и дефисы, до $MAX_SLUG знаков"
                throw TaskError.Failure(message = "slug «$slug» — $rule: task-scripts")
            }
            return BranchName(issue = issue, slug = slug, long = long)
        }

        /** Ветка задачи из имени текущей ветки или `null`, если это не ветка задачи. */
        fun parse(branch: String): BranchName? {
            val match = BRANCH.matchEntire(input = branch) ?: return null
            val (kind, issue, slug) = match.destructured
            return BranchName(issue = issue.toInt(), slug = slug, long = kind == LONG)
        }
    }
}

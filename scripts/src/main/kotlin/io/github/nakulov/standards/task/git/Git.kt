package io.github.nakulov.standards.task.git

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.Shell

/**
 * Операции `git`, нужные командам процесса. Ошибка `git` — [TaskError.Failure] с первой строкой его stderr.
 */
class Git(
    private val shell: Shell,
) {
    /** Папка — worktree, а не основной клон: `dev` здесь не переключается (#244 в ui-design-system). */
    fun isWorktree(): Boolean {
        val dirs = read(args = listOf("rev-parse", "--path-format=absolute", "--git-dir", "--git-common-dir")).lines()
        return dirs.first() != dirs.last()
    }

    fun isClean(): Boolean {
        return read(args = listOf("status", "--porcelain")).isEmpty()
    }

    fun currentBranch(): String {
        return read(args = listOf("branch", "--show-current"))
    }

    /** Свежий `origin` — база веток всегда `origin/dev`, а не локальный `dev` (#246 в ui-design-system). */
    fun fetch() {
        read(args = listOf("fetch", "--prune", "origin"))
    }

    private fun read(args: List<String>): String {
        val result = shell.run(command = listOf("git") + args)
        if (result.code != 0) {
            throw TaskError.Failure(message = "git ${args.first()}: ${result.err.trim().lineSequence().first()}")
        }
        return result.out.trim()
    }
}

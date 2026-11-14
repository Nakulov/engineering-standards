package io.github.nakulov.standards.task.git

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.Shell
import io.github.nakulov.standards.task.shell.ShellResult

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

    /** Корень репозитория — от него ищутся шаблоны задач `.github/ISSUE_TEMPLATE/`. */
    fun root(): String {
        return read(args = listOf("rev-parse", "--show-toplevel"))
    }

    fun currentBranch(): String {
        return read(args = listOf("branch", "--show-current"))
    }

    /** Свежий `origin` — база веток всегда `origin/dev`, а не локальный `dev` (#246 в ui-design-system). */
    fun fetch() {
        read(args = listOf("fetch", "--prune", "origin"))
    }

    fun hasLocalBranch(name: String): Boolean {
        return exists(ref = "refs/heads/$name")
    }

    fun hasRemoteBranch(name: String): Boolean {
        return exists(ref = "refs/remotes/origin/$name")
    }

    /** Новая ветка от [base] без отслеживания: `push -u` при PR задаст её `origin`. */
    fun createBranch(
        name: String,
        base: String,
    ) {
        read(args = listOf("switch", "--quiet", "--create", name, "--no-track", base))
    }

    /** Переключение на существующую ветку; есть только в `origin` — создаётся локальная с отслеживанием. */
    fun switchTo(name: String) {
        read(args = listOf("switch", "--quiet", name))
    }

    /** Отсоединиться на [ref] — так worktree уходит со своей ветки, не трогая `dev`. */
    fun detach(ref: String) {
        read(args = listOf("switch", "--quiet", "--detach", ref))
    }

    /** `dev` основного клона — до `origin/dev`, только перемоткой. */
    fun updateDev() {
        read(args = listOf("switch", "--quiet", "dev"))
        read(args = listOf("merge", "--quiet", "--ff-only", "origin/dev"))
    }

    /** Удаление с `-D`: после squash ветка не считается слитой, слияние PR проверяет вызывающий. */
    fun deleteBranch(name: String) {
        read(args = listOf("branch", "--quiet", "-D", name))
    }

    /** Отправка ветки в `origin` с отслеживанием. */
    fun push(name: String) {
        read(args = listOf("push", "--quiet", "--set-upstream", "origin", name))
    }

    /** База ветки — long-ветка, от которой её начали (`task start --from`); PR открывается в неё. */
    fun setBase(
        branch: String,
        base: String,
    ) {
        read(args = listOf("config", "branch.$branch.$BASE_KEY", base))
    }

    /** База ветки или `null` — тогда PR идёт в `dev`. */
    fun base(branch: String): String? {
        val result = run(args = listOf("config", "--get", "branch.$branch.$BASE_KEY"))
        return result.out.trim().takeIf { result.code == 0 }
    }

    private fun exists(ref: String): Boolean {
        return run(args = listOf("rev-parse", "--verify", "--quiet", ref)).code == 0
    }

    private fun read(args: List<String>): String {
        val result = run(args = args)
        if (result.code != 0) {
            throw TaskError.Failure(message = "git ${args.first()}: ${result.err.trim().lineSequence().first()}")
        }
        return result.out.trim()
    }

    private fun run(args: List<String>): ShellResult {
        return shell.run(command = listOf("git") + args)
    }

    private companion object {
        const val BASE_KEY = "task-base"
    }
}

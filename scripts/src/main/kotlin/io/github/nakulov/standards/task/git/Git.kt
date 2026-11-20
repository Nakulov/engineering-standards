package io.github.nakulov.standards.task.git

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.Shell
import io.github.nakulov.standards.task.shell.ShellResult
import java.io.File

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

    /**
     * Открытая ветка [name] — до `origin/[name]`, только перемоткой: обновляются и ссылка, и файлы (#61). Отставание
     * проверяет вызывающий; не перематывается — ошибка `git`.
     */
    fun fastForward(name: String) {
        read(args = listOf("merge", "--quiet", "--ff-only", "origin/$name"))
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

    /**
     * Long-ветка задачи [issue] — локальная или только в `origin`; нет — `null`. Нужна `task final` и `task done` из
     * ветки итогового PR `task/<N>-final` (#66).
     */
    fun longBranch(issue: Int): String? {
        val refs = listOf("$HEADS$LONG$issue-*", "$REMOTES$LONG$issue-*")
        return read(args = listOf("for-each-ref", "--format=%(refname)") + refs)
            .lines()
            .filter { line -> line.isNotBlank() }
            .map { line -> line.removePrefix(prefix = HEADS).removePrefix(prefix = REMOTES) }
            .firstOrNull()
    }

    /** Слияние [ref] в текущую ветку; не прошло (конфликт) — `false`, незавершённое слияние остаётся пользователю. */
    fun merge(ref: String): Boolean {
        return run(args = listOf("merge", "--quiet", "--no-edit", ref)).code == 0
    }

    /** Удаление ветки в `origin`: long-ветку после итогового PR из `task/<N>-final` (#66). */
    fun deleteRemoteBranch(name: String) {
        read(args = listOf("push", "--quiet", "origin", "--delete", name))
    }

    /** В репозитории есть хотя бы один коммит (`HEAD` указывает на коммит, а не на нерождённую ветку). */
    fun hasCommits(): Boolean {
        return exists(ref = "HEAD")
    }

    /** Первый коммит пустого репозитория — в ветку [branch], только [paths] (#8: файлы стандарта). */
    fun firstCommit(
        branch: String,
        message: String,
        paths: List<String>,
    ) {
        read(args = listOf("symbolic-ref", "HEAD", "refs/heads/$branch"))
        read(args = listOf("add", "--") + paths)
        read(args = listOf("commit", "--quiet", "--message", message))
    }

    /**
     * Изменённые пути рабочей папки: изменённые, удалённые, новые; переименование — двумя путями: старый удалён,
     * новый добавлен.
     */
    fun changedPaths(): List<String> {
        val args = listOf("status", "--porcelain", "-z", "--no-renames", "--untracked-files=all")
        val result = run(args = args)
        if (result.code != 0) {
            throw TaskError.Failure(message = "git status: ${result.err.trim().lineSequence().first()}")
        }
        val entries = result.out.split(NUL).filter { entry -> entry.isNotEmpty() }
        val paths = mutableListOf<String>()
        var index = 0
        while (index < entries.size) {
            val entry = entries[index]
            paths += entry.substring(startIndex = STATUS_PREFIX)
            index += if (entry.first() == RENAMED || entry.first() == COPIED) 2 else 1
        }
        return paths
    }

    /**
     * Коммит только [paths] с сообщением из [messageFile]: чужое из индекса в коммит не попадает. Пути — от корня
     * репозитория (`:/`, как в `git status --porcelain`): команда работает и из подпапки. В индекс добавляются только
     * пути, которые есть в рабочей папке; удалённые (и через `git rm`) коммит берёт сам.
     */
    fun commitPaths(
        paths: List<String>,
        messageFile: String,
    ) {
        val root = File(root())
        val present = paths.filter { path -> File(root, path).exists() }.map { path -> "$FROM_ROOT$path" }
        if (present.isNotEmpty()) {
            read(args = listOf("add", "--") + present)
        }
        read(args = listOf("commit", "--quiet", "--file", messageFile, "--") + paths.map { path -> "$FROM_ROOT$path" })
    }

    /** Общий `.git` — у основной папки и всех её worktree один. */
    fun commonDir(): String {
        return read(args = listOf("rev-parse", "--path-format=absolute", "--git-common-dir"))
    }

    /** Основная папка проекта — и из worktree: там лежат `plans/done/` и `plans/journal/`. */
    fun mainRoot(): String {
        return File(commonDir()).parentFile.path
    }

    /** Файлы, добавленные в ветке относительно общего предка с [base]. */
    fun addedPaths(base: String): List<String> {
        return read(args = listOf("diff", "--name-only", "--diff-filter=A", "$base...HEAD"))
            .lines()
            .filter { line -> line.isNotBlank() }
    }

    /** Удалить [paths] из ветки и закоммитить с [message]. */
    fun removeAndCommit(
        paths: List<String>,
        message: String,
    ) {
        read(args = listOf("rm", "--quiet", "--") + paths)
        read(args = listOf("commit", "--quiet", "--message", message))
    }

    /** Папка, где открыта ветка [branch] — основная или worktree; не открыта — `null`. */
    fun openedAt(branch: String): String? {
        var path: String? = null
        for (line in read(args = listOf("worktree", "list", "--porcelain")).lines()) {
            if (line.startsWith(prefix = WORKTREE)) {
                path = line.removePrefix(prefix = WORKTREE)
            } else if (line == "branch refs/heads/$branch") {
                return path
            }
        }
        return null
    }

    fun isAncestor(
        ancestor: String,
        descendant: String,
    ): Boolean {
        return run(args = listOf("merge-base", "--is-ancestor", ancestor, descendant)).code == 0
    }

    /** Временный worktree без ветки на [ref]: в нём `task plan` коммитит, не трогая рабочую папку (#53). */
    fun addWorktree(
        path: String,
        ref: String,
    ) {
        read(args = listOf("worktree", "add", "--quiet", "--detach", path, ref))
    }

    fun removeWorktree(path: String) {
        read(args = listOf("worktree", "remove", "--force", path))
    }

    /** Копия [path] на новой ветке [name] от [base] без отслеживания — как [createBranch] (#52). */
    fun addBranchWorktree(
        path: String,
        name: String,
        base: String,
    ) {
        read(args = listOf("worktree", "add", "--quiet", "--no-track", "-b", name, path, base))
    }

    /** Копия [path] на существующей ветке [name]; есть только в `origin` — создаётся локальная с отслеживанием. */
    fun addWorktreeOn(
        path: String,
        name: String,
    ) {
        read(args = listOf("worktree", "add", "--quiet", path, name))
    }

    /** [fastForward] в копии [path]: ветка открыта там, а не в текущей папке (#52). */
    fun fastForwardIn(
        path: String,
        name: String,
    ) {
        readIn(path = path, args = listOf("merge", "--quiet", "--ff-only", "origin/$name"))
    }

    /** Копия задачи со [slug] — `.worktrees/<slug>` основной папки, и при запуске из другой копии (#52). */
    fun copyPath(slug: String): String {
        return File(mainRoot(), "$WORKTREES/$slug").path
    }

    /**
     * Неотслеживаемые файлы папки [path], которые игнорирует git и которые подходят под шаблоны файла [listFile] — так
     * отбирает `.worktreeinclude` Claude Code (#52). Шаблоны разбирает git.
     */
    fun includedPaths(
        path: String,
        listFile: String,
    ): List<String> {
        val listed = readIn(path = path, args = listOf("ls-files", "--others", "--ignored", "--exclude-from=$listFile"))
            .lines()
            .filter { line -> line.isNotBlank() }
        if (listed.isEmpty()) {
            return emptyList()
        }
        val ignoredArgs = listOf("ls-files", "--others", "--ignored", "--exclude-standard", "--")
        return readIn(path = path, args = ignoredArgs + listed)
            .lines()
            .filter { line -> line.isNotBlank() }
    }

    /** Папки всех копий репозитория, с основной (#52). */
    fun worktrees(): List<String> {
        return read(args = listOf("worktree", "list", "--porcelain"))
            .lines()
            .filter { line -> line.startsWith(prefix = WORKTREE) }
            .map { line -> line.removePrefix(prefix = WORKTREE) }
    }

    /** Копию [path] — на существующую ветку [name]: копия long-ветки отсоединена после `task done` ветки блока (#52). */
    fun switchIn(
        path: String,
        name: String,
    ) {
        readIn(path = path, args = listOf("switch", "--quiet", name))
    }

    fun hasChanges(path: String): Boolean {
        return readIn(path = path, args = listOf("status", "--porcelain")).isNotEmpty()
    }

    fun commitAll(
        path: String,
        message: String,
    ) {
        readIn(path = path, args = listOf("add", "--all"))
        readIn(path = path, args = listOf("commit", "--quiet", "--message", message))
    }

    /** Коммит worktree [path] — в ветку [branch] в `origin`, без локальной ветки. */
    fun pushHead(
        path: String,
        branch: String,
    ) {
        readIn(path = path, args = listOf("push", "--quiet", "origin", "HEAD:refs/heads/$branch"))
    }

    fun head(path: String): String {
        return readIn(path = path, args = listOf("rev-parse", "HEAD"))
    }

    /**
     * Локальная ветка — на [commit] (хеш или ссылка); вызывающий проверил, что это перемотка и ветка нигде не
     * открыта.
     */
    fun moveBranch(
        branch: String,
        commit: String,
    ) {
        read(args = listOf("update-ref", "refs/heads/$branch", commit))
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

    /** `git -C [path]`: ошибка называет команду, а не `-C`. */
    private fun readIn(
        path: String,
        args: List<String>,
    ): String {
        val result = run(args = listOf("-C", path) + args)
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
        const val HEADS = "refs/heads/"
        const val REMOTES = "refs/remotes/origin/"
        const val LONG = "long/"
        const val NUL = '\u0000'
        const val STATUS_PREFIX = 3
        const val RENAMED = 'R'
        const val COPIED = 'C'
        const val FROM_ROOT = ":/"
        const val WORKTREE = "worktree "
        const val WORKTREES = ".worktrees"
    }
}

package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.BranchName
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.IssueInfo
import io.github.nakulov.standards.task.github.Issues
import java.io.File

/**
 * `task start <N> <slug> [--long | --from long/<M>-…] [--worktree]`: проверка готовности (#2, `take`, пункт 1 — статус
 * и блокеры), ветка от свежего `origin/dev` (или от long-ветки — для маленького PR в неё), статус In progress.
 * Long-ветка сразу отправляется в `origin`, чтобы от неё начинались части. Исследование — без ветки, эпик в работу не
 * берётся. Существующая ветка, отставшая от `origin`, перематывается; разошедшаяся — отказ до переключения (#61).
 * `--worktree` — ветка в копии `.claude/worktrees/<slug>` основной папки: основная папка не переключается и может быть
 * грязной, файлы из `.worktreeinclude` копируются при создании, последняя строка вывода — путь копии (#52). Ветка открыта в другой папке — отказ с путём.
 * Повторный запуск безопасен.
 */
class StartCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
    private val files: WorktreeFiles,
) : Command {
    override val usage: String =
        "<N> <slug> [--long | --from long/<M>-…] [--worktree] — ветка task/<N>-<slug> от origin/dev, статус " +
            "In progress; --worktree — в копии .claude/worktrees/<slug>"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val request = parse(args = args)
        val branch = request.branch
        if (!request.worktree && !git.isClean()) {
            throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
        }
        val info = issues.info(number = branch.issue)
        val item = board.item(issue = branch.issue, nodeId = issues.nodeId(number = branch.issue))
        checkReady(info = info, status = item.status)
        val copy = when {
            info.type == RESEARCH -> {
                research(request = request, console = console)
                null
            }
            request.worktree -> openCopy(request = request, console = console)
            else -> {
                switchToBranch(request = request, console = console)
                null
            }
        }
        if (item.status == Board.IN_PROGRESS) {
            console.line(text = "#${branch.issue} уже ${Board.IN_PROGRESS}")
        } else {
            board.move(item = item, status = Board.IN_PROGRESS)
            console.line(text = "#${branch.issue} → ${Board.IN_PROGRESS}")
        }
        if (copy != null) {
            console.line(text = copy)
        }
    }

    private fun parse(args: List<String>): Request {
        val fromIndex = args.indexOf(FROM_FLAG)
        val from = if (fromIndex >= 0) args.getOrNull(index = fromIndex + 1) else null
        val rest = if (fromIndex >= 0) args.take(n = fromIndex) + args.drop(n = fromIndex + 2) else args
        val long = LONG_FLAG in rest
        val worktree = WORKTREE_FLAG in rest
        val positional = rest - LONG_FLAG - WORKTREE_FLAG
        val issue = positional.firstOrNull()?.toIntOrNull()
        val badFrom = fromIndex >= 0 && (long || from == null || BranchName.parse(branch = from)?.long != true)
        if (positional.size != 2 || issue == null || badFrom) {
            throw TaskError.Failure(message = "использование: task start $usage")
        }
        return Request(
            branch = BranchName.of(issue = issue, slug = positional.last(), long = long),
            from = from,
            worktree = worktree,
        )
    }

    private fun checkReady(
        info: IssueInfo,
        status: String,
    ) {
        val reason = when {
            !info.open -> "закрыта"
            info.type == EPIC -> "— эпик: в работу берутся его подзадачи"
            status != Board.GROOMED && status != Board.IN_PROGRESS -> "в статусе «$status» — сначала груминг"
            info.openBlockers.isNotEmpty() -> "заблокирована: ${blockers(info = info)} открыты"
            else -> null
        }
        if (reason != null) {
            throw TaskError.Refusal(message = "#${info.number} $reason")
        }
    }

    private fun blockers(info: IssueInfo): String {
        return info.openBlockers.joinToString(separator = ", ") { blocker -> "#$blocker" }
    }

    private fun research(
        request: Request,
        console: Console,
    ) {
        val issue = request.branch.issue
        if (request.worktree) {
            throw TaskError.Refusal(message = "#$issue — исследование: ветка и копия не нужны")
        }
        console.line(text = "#$issue — исследование: ветка не нужна")
    }

    private fun switchToBranch(
        request: Request,
        console: Console,
    ) {
        val name = request.branch.toString()
        git.fetch()
        val opened = git.openedAt(branch = name)
        if (opened != null && !isSamePath(first = opened, second = git.root())) {
            throw openedRefusal(name = name, folder = opened)
        }
        val local = git.hasLocalBranch(name = name)
        val remote = git.hasRemoteBranch(name = name)
        if (local || remote) {
            switchToExisting(name = name, behind = local && remote && isBehind(name = name), console = console)
        } else {
            createBranch(request = request, copy = null, console = console)
        }
    }

    /** Ветка в копии `.claude/worktrees/<slug>` (#52); возвращает путь копии. */
    private fun openCopy(
        request: Request,
        console: Console,
    ): String {
        val name = request.branch.toString()
        val copy = git.copyPath(slug = request.branch.slug)
        git.fetch()
        val opened = git.openedAt(branch = name)
        if (opened != null) {
            if (!isSamePath(first = opened, second = copy)) {
                throw openedRefusal(name = name, folder = opened)
            }
            val behind = git.hasRemoteBranch(name = name) && isBehind(name = name)
            if (behind) {
                git.fastForwardIn(path = copy, name = name)
                console.line(text = "копия уже есть: $copy — перемотал до origin/$name")
            } else {
                console.line(text = "копия уже есть: $copy")
            }
            return copy
        }
        val local = git.hasLocalBranch(name = name)
        val remote = git.hasRemoteBranch(name = name)
        val occupied = File(copy).exists()
        if (local || remote) {
            val behind = local && remote && isBehind(name = name)
            if (occupied) {
                switchOwnCopy(copy = copy, name = name)
            } else {
                git.addWorktreeOn(path = copy, name = name)
            }
            if (behind) {
                git.fastForwardIn(path = copy, name = name)
                console.line(text = "ветка $name уже есть — копия $copy перемотана до origin/$name")
            } else {
                console.line(text = "ветка $name уже есть — копия $copy")
            }
            if (!occupied) {
                files.copyInto(copy = copy, console = console)
            }
        } else {
            if (occupied) {
                throw occupiedRefusal(copy = copy)
            }
            createBranch(request = request, copy = copy, console = console)
        }
        return copy
    }

    /**
     * Папка копии есть, ветка в ней не открыта: своя чистая копия (копия long-ветки после `task done` ветки блока)
     * переключается на ветку; чужая папка или правки — отказ (#52, изменено при плане).
     */
    private fun switchOwnCopy(
        copy: String,
        name: String,
    ) {
        val registered = git.worktrees().any { path -> isSamePath(first = path, second = copy) }
        if (!registered || git.hasChanges(path = copy)) {
            throw occupiedRefusal(copy = copy)
        }
        git.switchIn(path = copy, name = name)
    }

    private fun occupiedRefusal(copy: String): TaskError.Refusal {
        return TaskError.Refusal(message = "папка $copy занята — работайте там или удалите её")
    }

    private fun switchToExisting(
        name: String,
        behind: Boolean,
        console: Console,
    ) {
        git.switchTo(name = name)
        if (behind) {
            git.fastForward(name = name)
            console.line(text = "ветка $name уже есть — переключился и перемотал до origin/$name")
        } else {
            console.line(text = "ветка $name уже есть — переключился на неё")
        }
    }

    /** Локальная [name] отстала от `origin/[name]`; совпадает или впереди — нет; разошлась — отказ (#61). */
    private fun isBehind(name: String): Boolean {
        val local = "refs/heads/$name"
        val remote = "origin/$name"
        if (git.isAncestor(ancestor = remote, descendant = local)) {
            return false
        }
        if (!git.isAncestor(ancestor = local, descendant = remote)) {
            throw TaskError.Refusal(message = "локальная $name расходится с origin/$name — сведите вручную")
        }
        return true
    }

    /** Новая ветка — здесь ([copy] `null`) или сразу в копии [copy] (#52). */
    private fun createBranch(
        request: Request,
        copy: String?,
        console: Console,
    ) {
        val name = request.branch.toString()
        val from = request.from
        if (from != null && !git.hasRemoteBranch(name = from)) {
            throw TaskError.Refusal(message = "нет ветки origin/$from — сначала task start <N> <slug> --long")
        }
        val base = "origin/${from ?: DEV}"
        if (copy == null) {
            git.createBranch(name = name, base = base)
        } else {
            git.addBranchWorktree(path = copy, name = name, base = base)
        }
        if (from != null) {
            git.setBase(branch = name, base = from)
        }
        console.line(text = if (copy == null) "ветка $name от $base" else "ветка $name от $base — копия $copy")
        if (copy != null) {
            files.copyInto(copy = copy, console = console)
        }
        if (request.branch.long) {
            git.push(name = name)
            console.line(text = "ветка $name отправлена в origin")
        }
    }

    private fun openedRefusal(
        name: String,
        folder: String,
    ): TaskError.Refusal {
        return TaskError.Refusal(message = "ветка $name открыта в $folder — работайте там")
    }

    /** Пути из разных команд `git` (`worktree list`, `rev-parse`) сравниваются после разрешения ссылок. */
    private fun isSamePath(
        first: String,
        second: String,
    ): Boolean {
        return File(first).canonicalPath == File(second).canonicalPath
    }

    /** Разобранные аргументы: ветка задачи, long-ветка, от которой она начинается (`--from`), и `--worktree`. */
    private data class Request(
        val branch: BranchName,
        val from: String?,
        val worktree: Boolean,
    )

    private companion object {
        const val LONG_FLAG = "--long"
        const val FROM_FLAG = "--from"
        const val WORKTREE_FLAG = "--worktree"
        const val DEV = "dev"
        const val EPIC = "type:epic"
        const val RESEARCH = "type:research"
    }
}

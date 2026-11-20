package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.BranchName
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues
import java.io.File

/**
 * `task done` из ветки задачи после слияния её PR: свежий `dev` (в worktree — отсоединение на `origin/dev`, `dev` не
 * трогается), локальная ветка удаляется, задача → Done. Задача ещё открыта (PR с `Part of`) — статус не меняется.
 * Своя копия `.claude/worktrees/<slug>` удаляется последним шагом — после неё текущей папки нет; копия long-ветки на
 * ветке блока остаётся (#52). Из `task/<N>-final` удаляется и long-ветка задачи — локально и в `origin` (#66).
 */
class DoneCommand(
    private val git: Git,
    private val issues: Issues,
    private val board: Board,
) : Command {
    override val usage: String = "— после слияния PR: свежий dev, ветка удалена, статус Done"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        if (args.isNotEmpty()) {
            throw TaskError.Failure(message = "использование: task done $usage")
        }
        val name = git.currentBranch()
        val shown = name.ifEmpty { DETACHED }
        val branch = BranchName.parse(branch = name) ?: throw TaskError.Refusal(
            message = "$shown — не ветка задачи: task done запускается из task/<N>-… или long/<N>-…",
        )
        if (!git.isClean()) {
            throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
        }
        val pullRequest = issues.pullRequest(branch = name)
            ?: throw TaskError.Refusal(message = "у ветки $name нет PR — сначала task pr")
        if (!pullRequest.merged) {
            throw TaskError.Refusal(message = "PR #${pullRequest.number} не слит — сначала слияние")
        }
        val final = !branch.long && branch.slug == FinalCommand.SLUG
        val long = if (final) freeLong(issue = branch.issue) else null
        val worktree = leaveBranch(name = name, console = console)
        if (final) {
            removeLong(issue = branch.issue, name = long, console = console)
        }
        finishIssue(issue = branch.issue, console = console)
        if (worktree) {
            val slug = long?.let { found -> BranchName.parse(branch = found)?.slug } ?: branch.slug
            removeOwnCopy(slug = slug, console = console)
        }
    }

    private fun leaveBranch(
        name: String,
        console: Console,
    ): Boolean {
        git.fetch()
        val worktree = git.isWorktree()
        if (worktree) {
            git.detach(ref = BASE)
            console.line(text = "worktree отсоединён на $BASE, dev не тронут")
        } else {
            git.updateDev()
            console.line(text = "dev обновлён до $BASE")
        }
        git.deleteBranch(name = name)
        console.line(text = "ветка $name удалена")
        return worktree
    }

    /**
     * Long-ветка задачи для удаления после итогового PR из `task/<N>-final` (#66); нет — `null`. Открыта в другой
     * папке — отказ до удаления final: иначе `task done` уже не повторить.
     */
    private fun freeLong(issue: Int): String? {
        val name = git.longBranch(issue = issue) ?: return null
        val folder = git.openedAt(branch = name)
        if (folder != null) {
            throw TaskError.Refusal(message = "ветка $name открыта в $folder — переключите там ветку и повторите task done")
        }
        return name
    }

    /** Long-ветка [name] после итогового PR (#66) — удаляется локально и в `origin`, где есть. */
    private fun removeLong(
        issue: Int,
        name: String?,
        console: Console,
    ) {
        if (name == null) {
            console.line(text = "long-ветки задачи #$issue нет — удалена только ветка итогового PR")
            return
        }
        if (git.hasLocalBranch(name = name)) {
            git.deleteBranch(name = name)
        }
        if (git.hasRemoteBranch(name = name)) {
            git.deleteRemoteBranch(name = name)
        }
        console.line(text = "long-ветка $name удалена — локально и в origin")
    }

    private fun finishIssue(
        issue: Int,
        console: Console,
    ) {
        if (issues.info(number = issue).open) {
            console.line(text = "#$issue открыта — не последний PR задачи, статус остаётся")
            return
        }
        val item = board.item(issue = issue, nodeId = issues.nodeId(number = issue))
        if (item.status != Board.DONE) {
            board.move(item = item, status = Board.DONE)
        }
        console.line(text = "#$issue → ${Board.DONE}")
    }

    /**
     * Своя копия — `.claude/worktrees/<slug>` этой ветки (#52): удаляется изнутри, без `git -C` — защита сессии в ворктри
     * не пускает `git` в основную папку. Копия long-ветки, где слита ветка блока, остаётся. Копия ветки final — копия её
     * long-ветки (#66).
     */
    private fun removeOwnCopy(
        slug: String,
        console: Console,
    ) {
        val root = git.root()
        if (File(root).canonicalPath != File(git.copyPath(slug = slug)).canonicalPath) {
            return
        }
        git.removeWorktree(path = root)
        console.line(text = "копия $root удалена — текущей папки больше нет")
    }

    private companion object {
        const val BASE = "origin/dev"
        const val DETACHED = "HEAD отсоединён"
    }
}

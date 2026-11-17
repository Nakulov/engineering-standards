package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.Command
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.BranchName
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.IssueParts
import io.github.nakulov.standards.task.github.Issues
import java.io.File

/**
 * `task plan <N> <slug> [--long] --issue-parts <файл>` (#53): утверждённый план — в ветку задачи в `origin` через
 * временный worktree (рабочая папка и ветка пользователя не трогаются), без смены статуса и без PR; части плана —
 * в тело задачи между метками. Повтор — «Обновить план». Локальный план после сохранения удаляется.
 */
class PlanCommand(
    private val git: Git,
    private val issues: Issues,
    private val directory: File,
) : Command {
    override val usage: String =
        "<N> <slug> [--long] --issue-parts <файл> — план plans/<slug>.md в ветку задачи без смены статуса"

    override fun run(
        args: List<String>,
        console: Console,
    ) {
        val request = parse(args = args)
        val slug = request.branch.slug
        val number = request.branch.issue
        val branch = request.branch.toString()
        val plan = File(git.root(), "plans/$slug.md")
        if (!plan.isFile) {
            throw TaskError.Failure(message = "нет plans/$slug.md — сначала напишите план")
        }
        val title = plan.readLines()
            .firstOrNull { line -> line.startsWith(prefix = "# ") }
            ?.removePrefix(prefix = "# ")
            ?.trim()
            ?: throw TaskError.Failure(message = "в plans/$slug.md нет строки «# название»")
        val parts = File(request.parts).let { path -> if (path.isAbsolute) path else File(directory, request.parts) }
        if (!parts.isFile) {
            throw TaskError.Failure(message = "нет файла частей ${request.parts}")
        }
        val info = issues.info(number = number)
        if (!info.open) {
            throw TaskError.Refusal(message = "#$number закрыта")
        }
        if (info.type == EPIC) {
            throw TaskError.Refusal(message = "#$number — эпик: план — на подзадачу")
        }
        val planLine = "План: ветка `$branch`, `plans/$slug.md`"
        val body = IssueParts.merge(body = issues.body(number = number), parts = parts.readText(), planLine = planLine)
        git.fetch()
        val opened = git.openedAt(branch = branch)
        if (opened != null) {
            throw TaskError.Refusal(
                message = "ветка $branch открыта в $opened — выполнение начато, план правит исполнитель",
            )
        }
        val remote = git.hasRemoteBranch(name = branch)
        val local = git.hasLocalBranch(name = branch)
        if (local && !remote) {
            throw TaskError.Refusal(message = "локальная $branch не отправлена в origin — отправьте её или удалите")
        }
        if (local && !git.isAncestor(ancestor = "refs/heads/$branch", descendant = "origin/$branch")) {
            throw TaskError.Refusal(message = "локальная $branch расходится с origin/$branch — сведите вручную")
        }
        val commit = save(plan = plan, title = title, branch = branch, remote = remote, console = console)
        if (local) {
            git.moveBranch(branch = branch, commit = commit ?: "origin/$branch")
        }
        try {
            issues.editBody(number = number, body = body)
        } catch (error: TaskError) {
            throw TaskError.Failure(
                message = "план сохранён, тело задачи — нет: повторите task plan (${error.message})",
            )
        }
        plan.delete()
        console.line(text = "#$number: тело задачи обновлено, локальный plans/$slug.md удалён")
    }

    /** Коммит плана во временном worktree и push; план не изменился — `null`. Worktree удаляется всегда. */
    private fun save(
        plan: File,
        title: String,
        branch: String,
        remote: Boolean,
        console: Console,
    ): String? {
        val temp = File(git.commonDir(), "task-plan/${plan.nameWithoutExtension}").path
        git.addWorktree(path = temp, ref = if (remote) "origin/$branch" else BASE)
        try {
            plan.copyTo(target = File(temp, "plans/${plan.name}"), overwrite = true)
            if (!git.hasChanges(path = temp)) {
                console.line(text = "план не изменился — коммита нет")
                return null
            }
            val message = "${if (remote) "Обновить" else "Добавить"} план: $title"
            git.commitAll(path = temp, message = message)
            git.pushHead(path = temp, branch = branch)
            console.line(text = "$message → $branch")
            return git.head(path = temp)
        } finally {
            try {
                git.removeWorktree(path = temp)
            } catch (error: TaskError.Failure) {
                console.error(text = "удалите вручную: git worktree remove --force $temp")
            }
        }
    }

    private fun parse(args: List<String>): Request {
        val partsIndex = args.indexOf(PARTS_FLAG)
        val parts = if (partsIndex >= 0) args.getOrNull(index = partsIndex + 1) else null
        val rest = if (partsIndex >= 0) args.take(n = partsIndex) + args.drop(n = partsIndex + 2) else args
        val long = LONG_FLAG in rest
        val positional = rest - LONG_FLAG
        val issue = positional.firstOrNull()?.toIntOrNull()
        if (parts == null || positional.size != 2 || issue == null) {
            throw TaskError.Failure(message = "использование: task plan $usage")
        }
        return Request(branch = BranchName.of(issue = issue, slug = positional.last(), long = long), parts = parts)
    }

    /** Разобранные аргументы: ветка плана и файл частей задачи. */
    private data class Request(
        val branch: BranchName,
        val parts: String,
    )

    private companion object {
        const val LONG_FLAG = "--long"
        const val PARTS_FLAG = "--issue-parts"
        const val BASE = "origin/dev"
        const val EPIC = "type:epic"
    }
}

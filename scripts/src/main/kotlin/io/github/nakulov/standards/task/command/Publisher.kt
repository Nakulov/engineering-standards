package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues
import io.github.nakulov.standards.task.github.PrBody
import io.github.nakulov.standards.task.github.PullRequests

/**
 * Общее у `task pr` и `task final`: проверки, тело по шаблону, push, PR — новый или обновлённый (повтор безопасен).
 */
class Publisher(
    private val git: Git,
    private val issues: Issues,
    private val pullRequests: PullRequests,
    private val checks: CheckRunner,
) {
    fun publish(
        draft: PullRequestDraft,
        console: Console,
    ) {
        if (!git.isClean()) {
            throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
        }
        draft.checks.forEach { check ->
            checks.run(command = check)
            console.line(text = "проверка «$check» — прошла")
        }
        val body = PrBody.compose(text = draft.text, link = draft.link, checks = draft.checks)
        git.push(name = draft.head)
        val existing = issues.pullRequest(branch = draft.head)
        if (existing?.merged == true) {
            throw TaskError.Refusal(message = "PR #${existing.number} уже слит — task done")
        }
        val line = if (existing == null) {
            val url = pullRequests.create(
                base = draft.base,
                head = draft.head,
                title = draft.title,
                body = body,
                draft = draft.draft,
            )
            if (draft.draft) "PR (черновик): $url" else "PR: $url"
        } else {
            "PR обновлён: ${pullRequests.edit(head = draft.head, title = draft.title, body = body)}"
        }
        console.line(text = line)
    }
}

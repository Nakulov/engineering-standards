package io.github.nakulov.standards.task.github

/**
 * Запись PR через `gh`: создать, обновить, перевести из черновика. Ссылку на PR `gh` печатает сам.
 */
class PullRequests(
    private val gitHub: GitHub,
) {
    fun create(
        base: String,
        head: String,
        title: String,
        body: String,
        draft: Boolean,
    ): String {
        val args = listOf("pr", "create", "--base", base, "--head", head, "--title", title, "--body", body)
        return url(output = gitHub.write(args = if (draft) args + "--draft" else args))
    }

    fun edit(
        head: String,
        title: String,
        body: String,
    ): String {
        return url(output = gitHub.write(args = listOf("pr", "edit", head, "--title", title, "--body", body)))
    }

    fun ready(head: String) {
        gitHub.write(args = listOf("pr", "ready", head))
    }

    /** Первая строка тела PR — связь с задачей: `Closes #N` или `Part of #N`. */
    fun link(head: String): String {
        return gitHub.read(args = listOf("pr", "view", head, "--json", "body", "--jq", ".body | split(\"\\n\") | .[0]"))
    }

    private fun url(output: String): String {
        return output.lines().last()
    }
}

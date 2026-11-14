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

    /**
     * Связь с задачей из тела PR — строка `Closes #N` или `Part of #N` в любом месте (#41: первой строкой мог быть
     * комментарий шаблона, тело правят и в вебе); нет связи — пустая строка.
     */
    fun link(head: String): String {
        return gitHub.read(args = listOf("pr", "view", head, "--json", "body", "--jq", LINK_JQ))
    }

    private fun url(output: String): String {
        return output.lines().last()
    }

    private companion object {
        const val LINK_JQ = ".body | split(\"\\n\") | map(sub(\"\\r$\"; \"\")) | " +
            "map(select(test(\"^(Closes|Part of) #[0-9]+$\"))) | .[0] // \"\""
    }
}

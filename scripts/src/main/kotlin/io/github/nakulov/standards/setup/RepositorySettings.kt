package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.github.GitHub

/**
 * Флаги репозитория (#8, решение 5): только squash с заголовком и описанием PR, автоудаление веток, кнопка
 * «Update branch», wiki и discussions выключены, auto-merge выключен — сливает человек.
 */
class RepositorySettings(
    private val gitHub: GitHub,
) : Setup.Step {
    override fun run(mode: Setup.Mode): List<Finding> {
        val values = gitHub.read(args = listOf("api", REPOSITORY, "--jq", JQ)).split("\t")
        val actual = STANDARD.keys.zip(other = values).toMap()
        val differing = STANDARD.filter { (key, value) -> actual[key] != value }
        if (differing.isEmpty()) {
            return listOf(Finding(status = Finding.Status.OK, area = AREA, text = "соответствуют стандарту"))
        }
        if (mode.apply) {
            gitHub.write(args = listOf("api", "--method", "PATCH", REPOSITORY) + fields(differing = differing))
        }
        val status = if (mode.apply) Finding.Status.FIXED else Finding.Status.DEVIATION
        return differing.map { (key, value) ->
            Finding(status = status, area = AREA, text = "$key: ${actual[key]} → $value")
        }
    }

    /** Заголовок и тело squash-коммита GitHub принимает только парой — меняются вместе. */
    private fun fields(differing: Map<String, String>): List<String> {
        val squash = differing.keys.any { key -> key in SQUASH_MESSAGE }
        val keys = if (squash) differing.keys + SQUASH_MESSAGE else differing.keys
        return keys.flatMap { key ->
            val value = STANDARD.getValue(key)
            val flag = if (value == "true" || value == "false") "-F" else "-f"
            listOf(flag, "$key=$value")
        }
    }

    private companion object {
        const val AREA = "настройки"
        const val REPOSITORY = "repos/{owner}/{repo}"
        val SQUASH_MESSAGE = setOf("squash_merge_commit_title", "squash_merge_commit_message")
        val STANDARD = linkedMapOf(
            "allow_squash_merge" to "true",
            "allow_merge_commit" to "false",
            "allow_rebase_merge" to "false",
            "squash_merge_commit_title" to "PR_TITLE",
            "squash_merge_commit_message" to "PR_BODY",
            "delete_branch_on_merge" to "true",
            "allow_update_branch" to "true",
            "allow_auto_merge" to "false",
            "has_issues" to "true",
            "has_projects" to "true",
            "has_wiki" to "false",
            "has_discussions" to "false",
        )
        val JQ = "[${STANDARD.keys.joinToString(separator = ", ") { key -> ".$key" }}] | map(tostring) | @tsv"
    }
}

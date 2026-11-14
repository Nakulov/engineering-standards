package io.github.nakulov.standards.setup

fun makeSettingsArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}", "--jq", makeSettingsJq())
}

fun makeSettingsJq(): String {
    return "[.allow_squash_merge, .allow_merge_commit, .allow_rebase_merge, .squash_merge_commit_title, " +
        ".squash_merge_commit_message, .delete_branch_on_merge, .allow_update_branch, .allow_auto_merge, " +
        ".has_issues, .has_projects, .has_wiki, .has_discussions] | map(tostring) | @tsv"
}

fun makeStandardValues(): String {
    val values = listOf("true", "false", "false", "PR_TITLE", "PR_BODY", "true")
    return (values + listOf("true", "false", "true", "true", "false", "false")).joinToString(separator = "\t")
}

/** Тело squash-коммита — список коммитов, wiki включена. */
fun makeCommitMessagesValues(): String {
    val values = listOf("true", "false", "false", "PR_TITLE", "COMMIT_MESSAGES", "true")
    return (values + listOf("true", "false", "true", "true", "true", "false")).joinToString(separator = "\t")
}

fun makeSettingsOk(): Finding {
    return Finding(status = Finding.Status.OK, area = "настройки", text = "соответствуют стандарту")
}

fun makeSettingsDeviations(): List<Finding> {
    return listOf(
        Finding(
            status = Finding.Status.DEVIATION,
            area = "настройки",
            text = "squash_merge_commit_message: COMMIT_MESSAGES → PR_BODY",
        ),
        Finding(status = Finding.Status.DEVIATION, area = "настройки", text = "has_wiki: true → false"),
    )
}

fun makeSettingsFixes(): List<Finding> {
    return makeSettingsDeviations().map { finding -> finding.copy(status = Finding.Status.FIXED) }
}

fun makeSettingsPatchArgs(): List<String> {
    return listOf(
        "api",
        "--method",
        "PATCH",
        "repos/{owner}/{repo}",
        "-f",
        "squash_merge_commit_message=PR_BODY",
        "-F",
        "has_wiki=false",
        "-f",
        "squash_merge_commit_title=PR_TITLE",
    )
}

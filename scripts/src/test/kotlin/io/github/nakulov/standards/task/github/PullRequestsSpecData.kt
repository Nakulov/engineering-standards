package io.github.nakulov.standards.task.github

fun makePullUrl(): String {
    return "https://github.com/owner/repo/pull/32"
}

/** `gh pr create` перед ссылкой может напечатать служебные строки. */
fun makeCreateOutput(): String {
    return "Warning: 1 uncommitted change\nhttps://github.com/owner/repo/pull/32"
}

fun makeCreateDraftArgs(): List<String> {
    return listOf(
        "pr",
        "create",
        "--base",
        "dev",
        "--head",
        "task/13-x",
        "--title",
        "Заголовок",
        "--body",
        "Тело",
        "--draft",
    )
}

fun makeCreateArgs(): List<String> {
    return listOf("pr", "create", "--base", "dev", "--head", "long/13-x", "--title", "Заголовок", "--body", "Тело")
}

fun makePrEditArgs(): List<String> {
    return listOf("pr", "edit", "task/13-x", "--title", "Заголовок", "--body", "Тело")
}

fun makeLinkArgs(): List<String> {
    val jq = ".body | split(\"\\n\") | map(sub(\"\\r$\"; \"\")) | " +
        "map(select(test(\"^(Closes|Part of) #[0-9]+$\"))) | .[0] // \"\""
    return listOf("pr", "view", "task/13-x", "--json", "body", "--jq", jq)
}

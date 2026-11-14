package io.github.nakulov.standards.task.command

fun makePartDraft(base: String): PullRequestDraft {
    return PullRequestDraft(
        head = "task/13-x",
        base = base,
        link = "Part of #13",
        title = "Заголовок",
        text = makeBodyText(),
        checks = listOf("./gradlew test", "scripts/task --help"),
        draft = true,
    )
}

fun makeNoPrMessage(): String {
    return "у ветки task/13-x нет PR — сначала task pr --title … --body-file …"
}

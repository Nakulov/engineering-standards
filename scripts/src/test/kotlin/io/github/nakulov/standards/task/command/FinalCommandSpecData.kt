package io.github.nakulov.standards.task.command

fun makeFinalDraft(): PullRequestDraft {
    return PullRequestDraft(
        head = "long/13-x",
        base = "dev",
        link = "Closes #13",
        title = "Заголовок",
        text = makeBodyText(),
        checks = listOf("./gradlew test", "scripts/task --help"),
        draft = false,
    )
}

fun makeFinalUsageMessage(): String {
    return "использование: task final <N> --title «…» --body-file <файл> [--check «команда»]… — " +
        "итоговый PR long/<N>-… → dev"
}

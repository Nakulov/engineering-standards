package io.github.nakulov.standards.task.command

fun makeFinalDraft(): PullRequestDraft {
    return PullRequestDraft(
        head = "task/13-final",
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
        "итоговый PR task/<N>-final → dev из long/<N>-…"
}

fun makeFinalWrongBranchMessage(): String {
    return "task final 13 запускается из long/13-… или task/13-final, сейчас «task/13-x»"
}

fun makeFinalMergeMessage(): String {
    return "слияние origin/long/13-x в task/13-final не прошло — разрешите конфликт и закоммитьте или проверьте, " +
        "что origin/long/13-x существует; затем повторите task final 13"
}

fun makeFinalDirtyMessage(): String {
    return "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их"
}

package io.github.nakulov.standards.task.command

import java.io.File

/** Путь к образцу текста PR из `src/test/resources`. */
fun makeBodyPath(): String {
    return File(checkNotNull(Thread.currentThread().contextClassLoader.getResource("pr/body.md")).toURI()).path
}

fun makeBodyText(): String {
    return File(makeBodyPath()).readText()
}

fun makeDraftArgs(): List<String> {
    return listOf(
        "--title",
        "Заголовок",
        "--body-file",
        makeBodyPath(),
        "--check",
        "./gradlew test",
        "--check",
        "scripts/task --help",
    )
}

fun makeParsedDraft(): PullRequestDraft {
    return PullRequestDraft(
        head = "task/13-x",
        base = "dev",
        link = "Closes #13",
        title = "Заголовок",
        text = makeBodyText(),
        checks = listOf("./gradlew test", "scripts/task --help"),
        draft = true,
    )
}

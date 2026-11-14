package io.github.nakulov.standards.task.command

import java.io.File

/** Настоящий шаблон этапа плагина. */
fun makeStageTemplateFile(): File {
    return File(System.getProperty("task.repoRoot"), "templates/github/stage.md")
}

fun makeResourcePath(path: String): String {
    return File(checkNotNull(Thread.currentThread().contextClassLoader.getResource(path)) { path }.toURI()).path
}

fun makeStageArgs(file: String): List<String> {
    return listOf("create", "--title", "Этап 1", "--body-file", makeResourcePath(path = file))
}

fun makeMilestoneArgs(): List<String> {
    return listOf(
        "api",
        "--method",
        "POST",
        "repos/{owner}/{repo}/milestones",
        "-f",
        "title=Этап 1",
        "-f",
        "description=" + File(makeResourcePath(path = "stage/full.md")).readText().trim(),
        "-f",
        "due_on=2026-12-01T00:00:00Z",
        "--jq",
        ".html_url",
    )
}

fun makeEmptySectionsMessage(): String {
    return "в описании этапа пусты разделы: «Критерий завершения», «Не входит», «Эпики и порядок» — заполните, " +
        "пустой — «Нет»"
}

fun makeStageUsageMessage(): String {
    return "использование: task stage create --title «…» --body-file <файл> [--due ГГГГ-ММ-ДД] — веха этапа по шаблону"
}

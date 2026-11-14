package io.github.nakulov.standards.task.command

fun makeNewArgs(): List<String> {
    return listOf("task", "--title", "Новая задача", "--body-file", makeBodyPath())
}

fun makeUnknownTypeMessage(): String {
    return "нет шаблона .github/ISSUE_TEMPLATE/story.md — типы: bug, epic, research, task"
}

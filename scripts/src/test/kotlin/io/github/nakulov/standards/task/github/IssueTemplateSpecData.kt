package io.github.nakulov.standards.task.github

import java.io.File

/** Настоящий шаблон задачи типа [type] из `.github/ISSUE_TEMPLATE/` этого репозитория. */
fun makeIssueTemplate(type: String): String {
    return File(System.getProperty("task.repoRoot"), ".github/ISSUE_TEMPLATE/$type.md").readText()
}

fun makeNoTypeLabelMessage(): String {
    return "в заголовке task.md нет метки типа — нужна строка labels: [type:…]"
}

fun makeTwoLabelsTemplate(): String {
    return "---\nname: Задача\nlabels: [process, type:task]\n---\nтекст"
}

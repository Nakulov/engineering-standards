package io.github.nakulov.standards.task

import java.io.File

private val COMMENT = Regex(pattern = "<!--.*?-->", option = RegexOption.DOT_MATCHES_ALL)
private val CONCRETE = Regex(pattern = "[0-9]{4}-[0-9]{2}-[0-9]{2}|#[0-9]+")

/** Шаблон → даты и номера задач вне комментариев; пустая карта — шаблоны без конкретного содержимого. */
fun findConcreteContent(): Map<String, List<String>> {
    val root = File(System.getProperty("task.repoRoot"))
    val templates = File(root, "templates").walkTopDown().filter { file -> file.isFile }.sortedBy { file -> file.path }
    val hits = templates.associate { file ->
        val text = COMMENT.replace(input = file.readText(), replacement = "")
        file.relativeTo(root).path to CONCRETE.findAll(input = text).map { match -> match.value }.toList()
    }
    return hits.filterValues { found -> found.isNotEmpty() }
}

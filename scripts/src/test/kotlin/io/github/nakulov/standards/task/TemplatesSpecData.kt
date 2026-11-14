package io.github.nakulov.standards.task

import io.github.nakulov.standards.setup.Labels
import io.github.nakulov.standards.task.github.IssueTemplate
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

/** Шаблоны `templates/github/ISSUE_TEMPLATE/`, чьей метки типа нет в `templates/github/labels.yml`. */
fun findTemplatesWithoutLabel(): List<String> {
    val github = File(System.getProperty("task.repoRoot"), "templates/github")
    val labels = Labels.parse(text = File(github, "labels.yml").readText(), file = "labels.yml")
    val names = labels.map { label -> label.name }
    val templates = File(github, "ISSUE_TEMPLATE").listFiles { file -> file.extension == "md" }.orEmpty()
        .sortedBy { file -> file.name }
    return templates.filter { file -> IssueTemplate.typeLabel(text = file.readText(), file = file.name) !in names }
        .map { file -> file.name }
}

package io.github.nakulov.standards.plugin

import java.io.File

// Ссылка markdown `](путь)` или `](путь#якорь)`: группа 1 — путь без якоря.
private val LINK = Regex(pattern = "\\]\\(([^)#\\s]+)[^)]*\\)")

private fun docs(): File = File(System.getProperty("task.repoRoot"), "docs")

/** Ссылки оглавления `docs/README.md` — пути от `docs/`, без якорей; внешние (`http…`) не берём. */
private fun indexLinks(): List<String> =
    LINK.findAll(input = File(docs(), "README.md").readText())
        .map { match -> match.groupValues[1] }
        .filterNot { link -> link.startsWith(prefix = "http") }
        .toList()

/** Ссылки оглавления на несуществующие файлы. */
fun findBrokenIndexLinks(): List<String> = indexLinks().filterNot { link -> File(docs(), link).exists() }

/** Разделы банка — папки `docs/` со своим `README.md`, кроме самого `docs/`; пути от `docs/`, по алфавиту. */
fun findBankSections(): List<String> =
    docs().walkTopDown()
        .filter { dir -> dir != docs() && File(dir, "README.md").isFile }
        .map { dir -> dir.relativeTo(docs()).path }
        .sorted()
        .toList()

/** Разделы банка, на чей `README.md` оглавление не ссылается. */
fun findSectionsMissingFromIndex(): List<String> {
    val links = indexLinks().toSet()
    return findBankSections().filterNot { section -> "$section/README.md" in links }
}

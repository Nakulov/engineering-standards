package io.github.nakulov.standards.plugin

import java.io.File

// Ссылка markdown `](путь)` или `](путь#якорь)`: группа 1 — путь без якоря.
private const val LINK_PATTERN = "\\]\\(([^)#\\s]+)[^)]*\\)"

// Исключение KT-TEST-6: тест проверяет файлы репозитория, а не код, поэтому прятать за интерфейс нечего.
// Ошибки оглавления `docs/README.md`: ссылка на несуществующий файл и раздел банка без ссылки на его `README.md`.
// Внешние ссылки (`http…`) не проверяются.
fun findBankIndexErrors(): List<String> {
    val docs = File(System.getProperty(/* key = */ "task.repoRoot"), /* child = */ "docs")
    val links = Regex(pattern = LINK_PATTERN).findAll(input = File(docs, /* child = */ "README.md").readText())
        .map { match -> match.groupValues[1] }
        .filterNot { link -> link.startsWith(prefix = "http") }
        .toList()
    val brokenLinks = links.filterNot { link -> File(docs, link).exists() }
        .map { link -> "битая ссылка: $link" }
    val missingSections = findBankSections().filterNot { section -> "$section/README.md" in links }
        .map { section -> "раздел без строки: $section" }
    return brokenLinks + missingSections
}

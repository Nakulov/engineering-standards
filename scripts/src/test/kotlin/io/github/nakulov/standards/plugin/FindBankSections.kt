package io.github.nakulov.standards.plugin

import java.io.File

// Исключение KT-TEST-6: тест проверяет файлы репозитория, а не код, поэтому прятать за интерфейс нечего.
// Разделы банка — папки `docs/` со своим `README.md`, кроме самого `docs/`; пути от `docs/`, по алфавиту.
fun findBankSections(): List<String> {
    val docs = File(System.getProperty(/* key = */ "task.repoRoot"), /* child = */ "docs")
    return docs.walkTopDown()
        .filter { dir -> dir != docs && File(dir, /* child = */ "README.md").isFile }
        .map { dir ->
            dir.relativeTo(docs)
                .path
        }
        .sorted()
        .toList()
}

package io.github.nakulov.standards.plugin

import java.io.File

// Вне стандарта: функция читает и разбирает файлы репозитория прямо в тесте, без интерфейса —
// проверяются файлы, а не код.
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

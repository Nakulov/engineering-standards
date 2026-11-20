package io.github.nakulov.standards.task.command

import java.io.File
import java.nio.file.Files

/**
 * Папка запуска: `message.txt` по шаблону (файлы `scripts/a.kt`, `scripts/b.kt`) и `no-plan-task.txt` —
 * без «Задачи плана» (файл `scripts/a.kt`).
 */
fun makeCommitDirectory(): File {
    val base = File(System.getProperty("task.cache"), "commit")
    base.mkdirs()
    val directory = Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "run").toFile()
    File(directory, "message.txt").writeText(makeCommitText())
    File(directory, "no-plan-task.txt").writeText(makeNoPlanTaskCommitText())
    return directory
}

package io.github.nakulov.standards.task.cli

import java.io.File
import java.nio.file.Files

/** Временная текущая папка `task` — своя на каждый тест. */
fun makeCurrentDirectory(): File {
    val base = File(System.getProperty("task.cache"), "cli")
    base.mkdirs()
    return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "current").toFile()
}

/** Папка копии внутри [current] — создаётся. */
fun makeCopyDirectory(current: File): File {
    val copy = File(current, "copy")
    copy.mkdirs()
    return copy
}

fun makeCommitArgs(): List<String> {
    return listOf("commit", "--message-file", "plans/journal/slug-commit.txt")
}

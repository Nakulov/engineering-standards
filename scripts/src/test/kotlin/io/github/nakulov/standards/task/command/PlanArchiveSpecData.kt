package io.github.nakulov.standards.task.command

import java.io.File
import java.nio.file.Files

/** Папка ветки с `plans/commit-commands.md` и пустая основная папка — во временной папке модуля. */
fun makeArchiveFolders(): Pair<File, File> {
    val base = File(System.getProperty("task.cache"), "archive")
    base.mkdirs()
    val root = Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "run").toFile()
    val branch = File(root, "worktree")
    File(branch, "plans").mkdirs()
    File(branch, "plans/commit-commands.md").writeText("# Команды коммита\n\n- [x] Задача 1\n")
    val main = File(root, "main")
    main.mkdirs()
    return branch to main
}

package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.git.Git
import java.io.File

/**
 * Настройка копии задачи (#52): неотслеживаемые файлы основной папки, которые игнорирует git и которые подходят под
 * шаблоны `.worktreeinclude` (его понимает и Claude Code), копируются в копию — без них не собрать
 * (`local.properties`). Нет `.worktreeinclude` — ничего. Строка-путь без шаблона, которого нет в основной папке, —
 * строка в выводе.
 */
class WorktreeFiles(
    private val git: Git,
) {
    fun copyInto(
        copy: String,
        console: Console,
    ) {
        val main = File(git.mainRoot())
        val list = File(main, LIST)
        if (!list.isFile) {
            return
        }
        val paths = git.includedPaths(path = main.path, listFile = list.path)
        for (path in paths) {
            File(main, path).copyTo(target = File(copy, path), overwrite = true)
        }
        if (paths.isNotEmpty()) {
            console.line(text = "из $LIST скопировано: ${paths.joinToString(separator = ", ")}")
        }
        for (line in missing(list = list, main = main)) {
            console.line(text = "$LIST: $line — нет в основной папке, пропущено")
        }
    }

    /** Строки-пути без шаблонных символов, комментариев и исключений, которых нет в основной папке. */
    private fun missing(
        list: File,
        main: File,
    ): List<String> {
        return list.readLines()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() && line.first() !in SPECIAL && line.none { char -> char in PATTERN } }
            .filter { line -> !File(main, line.removeSuffix(suffix = "/")).exists() }
    }

    private companion object {
        const val LIST = ".worktreeinclude"
        const val SPECIAL = "#!"
        const val PATTERN = "*?["
    }
}

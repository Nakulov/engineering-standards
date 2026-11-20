package io.github.nakulov.standards.task.cli

import java.io.File

/**
 * Опция `-C <папка>` перед командой, как у `git -C` (#98): `task` работает в указанной папке — агенту не нужен `cd`.
 * Без опции — текущая папка [current]; относительный путь — от неё. `-C` после имени команды — аргумент команды.
 */
class WorkingDirectory(
    private val current: File,
) {
    fun split(args: List<String>): Invocation {
        if (args.firstOrNull() != OPTION) {
            return Invocation(directory = current, args = args)
        }
        val path = args.getOrNull(index = 1)
            ?: throw TaskError.Failure(message = "$OPTION без папки — task $OPTION <папка> <команда>")
        val directory = File(path).let { file -> if (file.isAbsolute) file else File(current, path) }
        if (!directory.isDirectory) {
            throw TaskError.Failure(message = "$OPTION $path: нет такой папки — проверьте путь")
        }
        return Invocation(directory = directory, args = args.drop(n = 2))
    }

    /** Папка, в которой работает `task`, и аргументы без опции `-C`. */
    data class Invocation(
        val directory: File,
        val args: List<String>,
    )

    private companion object {
        const val OPTION = "-C"
    }
}

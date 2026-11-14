package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.git.Git
import java.io.File

/**
 * Общий блок `.gitignore` (#8, решение 7): между метками, в начале файла — строки проекта идут после него и могут
 * переопределить общие через `!`. Сверяется и переписывается только блок, строки проекта не трогаются.
 */
class GitignoreBlock(
    private val git: Git,
    private val block: String,
) : Setup.Step {
    override fun run(mode: Setup.Mode): List<Finding> {
        val file = File(git.root(), GITIGNORE)
        val lines = if (file.isFile) file.readText().lines() else emptyList()
        val start = lines.indexOf(element = START)
        val end = lines.indexOf(element = END)
        if ((start == -1) != (end == -1) || end < start) {
            val text = "$GITIGNORE: метки блока без пары — поправьте вручную: $START … $END"
            return listOf(Finding(status = Finding.Status.DEVIATION, area = AREA, text = text))
        }
        val expected = listOf(START) + block.trimEnd().lines() + END
        val current = if (start == -1) emptyList() else lines.subList(fromIndex = start, toIndex = end + 1)
        val rest = if (start == -1) lines else lines.subList(fromIndex = 0, toIndex = start) + lines.drop(n = end + 1)
        val project = rest.dropWhile { line -> line.isBlank() }.dropLastWhile { line -> line.isBlank() }
        if (current == expected && start == 0) {
            return listOf(Finding(status = Finding.Status.OK, area = AREA, text = "$GITIGNORE: общий блок"))
        }
        val state = when {
            start == -1 -> "нет общего блока"
            current != expected -> "общий блок отличается от стандарта"
            else -> "общий блок не в начале файла"
        }
        if (!mode.apply) {
            return listOf(Finding(status = Finding.Status.DEVIATION, area = AREA, text = "$GITIGNORE: $state"))
        }
        val tail = if (project.isEmpty()) emptyList() else listOf("") + project
        file.writeText((expected + tail).joinToString(separator = "\n", postfix = "\n"))
        val text = "$GITIGNORE: $state — блок записан, закоммитьте через задачу"
        return listOf(Finding(status = Finding.Status.FIXED, area = AREA, text = text))
    }

    private companion object {
        const val AREA = "файлы"
        const val GITIGNORE = ".gitignore"
        const val START = "# >>> engineering-standards"
        const val END = "# <<< engineering-standards"
    }
}

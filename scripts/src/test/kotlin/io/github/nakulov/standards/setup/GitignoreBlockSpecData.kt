package io.github.nakulov.standards.setup

import java.io.File
import java.nio.file.Files

/** Пустая папка проекта в `build/` модуля. */
fun makeProjectRoot(): File {
    val base = File(System.getProperty("task.cache"), "setup")
    base.mkdirs()
    return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "project").toFile()
}

fun makeBlock(): String {
    return "# общий блок\n.DS_Store\n.idea/*\n"
}

/** Строки проекта: исключение общего файла IDE и своё правило. */
fun makeProjectLines(): String {
    return "!.idea/jsonSchemas.xml\nlogs/\n"
}

fun makeBlockOnly(): String {
    return "# >>> engineering-standards\n# общий блок\n.DS_Store\n.idea/*\n# <<< engineering-standards\n"
}

fun makeBlockWithProjectLines(): String {
    return makeBlockOnly() + "\n" + makeProjectLines()
}

fun makeOutdatedBlockWithProjectLines(): String {
    return "# >>> engineering-standards\n.DS_Store\n# <<< engineering-standards\n\n" + makeProjectLines()
}

fun writeGitignore(
    root: File,
    text: String,
) {
    File(root, ".gitignore").writeText(text)
}

fun readGitignore(root: File): String {
    return File(root, ".gitignore").readText()
}

/** [text] — состояние для отклонения; `null` — блок по стандарту. */
fun makeGitignoreFinding(
    status: Finding.Status,
    text: String?,
): Finding {
    return Finding(status = status, area = "файлы", text = ".gitignore: ${text ?: "общий блок"}")
}

fun makeBrokenMarkersFinding(): Finding {
    val text = ".gitignore: метки блока без пары — поправьте вручную: # >>> engineering-standards … " +
        "# <<< engineering-standards"
    return Finding(status = Finding.Status.DEVIATION, area = "файлы", text = text)
}

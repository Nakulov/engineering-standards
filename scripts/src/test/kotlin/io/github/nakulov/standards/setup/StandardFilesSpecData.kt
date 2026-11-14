package io.github.nakulov.standards.setup

import java.io.File

private const val AREA = "файлы"

/** Корень плагина с двумя шаблонами задач, шаблоном PR и `.editorconfig`. */
fun makePluginRoot(): File {
    val plugin = makeProjectRoot()
    writeProjectFile(root = plugin, path = "templates/github/ISSUE_TEMPLATE/task.md", text = "шаблон задачи\n")
    val config = "blank_issues_enabled: false\n"
    writeProjectFile(root = plugin, path = "templates/github/ISSUE_TEMPLATE/config.yml", text = config)
    writeProjectFile(root = plugin, path = "templates/github/pull_request_template.md", text = "Closes #\n")
    writeProjectFile(root = plugin, path = "templates/project/editorconfig", text = "root = true\n")
    return plugin
}

fun writeProjectFile(
    root: File,
    path: String,
    text: String,
) {
    val file = File(root, path)
    file.parentFile.mkdirs()
    file.writeText(text)
}

/** Файлы проекта: путь → содержимое. */
fun readProjectFiles(root: File): Map<String, String> {
    return root.walkTopDown().filter { file -> file.isFile }.associate { file ->
        file.relativeTo(root).path to file.readText()
    }
}

fun makeMissingFindings(): List<Finding> {
    return listOf(
        Finding(status = Finding.Status.DEVIATION, area = AREA, text = ".github/ISSUE_TEMPLATE/config.yml: нет"),
        Finding(
            status = Finding.Status.DEVIATION,
            area = AREA,
            text = ".github/ISSUE_TEMPLATE/task.md: отличается от стандарта",
        ),
        Finding(status = Finding.Status.DEVIATION, area = AREA, text = ".github/pull_request_template.md: нет"),
        Finding(status = Finding.Status.DEVIATION, area = AREA, text = ".editorconfig: нет"),
        Finding(
            status = Finding.Status.NOTE,
            area = AREA,
            text = "шаблон вне стандарта: .github/ISSUE_TEMPLATE/story.md",
        ),
    )
}

fun makeCopiedFiles(): Map<String, String> {
    return mapOf(
        ".github/ISSUE_TEMPLATE/config.yml" to "blank_issues_enabled: false\n",
        ".github/ISSUE_TEMPLATE/task.md" to "шаблон задачи\n",
        ".github/pull_request_template.md" to "Closes #\n",
        ".editorconfig" to "root = true\n",
    )
}

fun makeFilesOk(): Finding {
    return Finding(status = Finding.Status.OK, area = AREA, text = "копии стандарта совпадают (4)")
}

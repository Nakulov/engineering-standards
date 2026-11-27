package io.github.nakulov.standards.setup

import java.io.File

const val COPIES_AREA = "копии"
private const val COPY_SOURCE = "templates/a.txt"
private const val COPY_TARGET = "a.txt"
private const val NESTED_TARGET = "docs/nested/a.txt"
private const val STANDARD_TEXT = "стандарт\n"
private const val OLD_TEXT = "старый\n"

/** Корень плагина с одним шаблоном `templates/a.txt`. */
fun makeCopiesPluginRoot(): File {
    val plugin = makeProjectRoot()
    writeProjectFile(root = plugin, path = COPY_SOURCE, text = STANDARD_TEXT)
    return plugin
}

fun makeEmptyProject(): File {
    return makeProjectRoot()
}

fun makeProjectWithOldCopy(): File {
    return makeProjectWithCopy(text = OLD_TEXT)
}

fun makeProjectWithStandardCopy(): File {
    return makeProjectWithCopy(text = STANDARD_TEXT)
}

fun makeFlatCopies(): List<Pair<String, String>> {
    return listOf(COPY_SOURCE to COPY_TARGET)
}

fun makeNestedCopies(): List<Pair<String, String>> {
    return listOf(COPY_SOURCE to NESTED_TARGET)
}

fun makeMissingCopyFindings(): List<Finding> {
    return listOf(makeCopyFinding(status = Finding.Status.DEVIATION, text = "a.txt: нет"))
}

fun makeDifferingCopyFindings(): List<Finding> {
    return listOf(makeCopyFinding(status = Finding.Status.DEVIATION, text = "a.txt: отличается от стандарта"))
}

fun makeWrittenMissingCopyFindings(): List<Finding> {
    return listOf(
        makeCopyFinding(status = Finding.Status.FIXED, text = "a.txt записан (нет) — закоммитьте через задачу"),
    )
}

fun makeWrittenDifferingCopyFindings(): List<Finding> {
    return listOf(
        makeCopyFinding(
            status = Finding.Status.FIXED,
            text = "a.txt записан (отличается от стандарта) — закоммитьте через задачу",
        ),
    )
}

fun makeFlatFiles(): Map<String, String> {
    return mapOf(COPY_TARGET to STANDARD_TEXT)
}

fun makeNestedFiles(): Map<String, String> {
    return mapOf(NESTED_TARGET to STANDARD_TEXT)
}

private fun makeProjectWithCopy(text: String): File {
    val project = makeProjectRoot()
    writeProjectFile(root = project, path = COPY_TARGET, text = text)
    return project
}

private fun makeCopyFinding(status: Finding.Status, text: String): Finding {
    return Finding(status = status, area = COPIES_AREA, text = text)
}

package io.github.nakulov.standards.setup

import java.io.File

internal class StandardCopies(
    private val pluginRoot: File,
    private val area: String,
) {

    fun run(
        root: File,
        copies: List<Pair<String, String>>,
        mode: Setup.Mode,
    ): List<Finding> {
        val differing = copies.filter { (source, target) ->
            val file = File(root, target)
            !file.isFile || file.readText() != File(pluginRoot, source).readText()
        }
        return differing.map { (source, target) ->
            val file = File(root, target)
            val state = if (file.isFile) "отличается от стандарта" else "нет"
            if (mode.apply) {
                file.parentFile.mkdirs()
                file.writeText(File(pluginRoot, source).readText())
                val text = "$target записан ($state) — закоммитьте через задачу"
                Finding(status = Finding.Status.FIXED, area = area, text = text)
            } else {
                Finding(status = Finding.Status.DEVIATION, area = area, text = "$target: $state")
            }
        }
    }
}

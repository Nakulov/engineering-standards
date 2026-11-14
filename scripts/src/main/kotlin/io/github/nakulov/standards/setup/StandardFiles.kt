package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.git.Git
import java.io.File

/**
 * Точные копии файлов стандарта в проекте (#8, решение 7): шаблоны задач и PR в `.github/` (GitHub читает их только
 * из репозитория), `config.yml` с запретом задач без шаблона, `.editorconfig` (IDE читает его только из проекта).
 * Файлы пишутся в рабочую копию — в проект они попадают через задачу и PR; шаблон вне стандарта только называется.
 */
class StandardFiles(
    private val git: Git,
    private val pluginRoot: File,
) : Setup.Step {
    override fun run(mode: Setup.Mode): List<Finding> {
        val root = File(git.root())
        val pairs = copies()
        val differing = pairs.filter { (source, target) ->
            val file = File(root, target)
            !file.isFile || file.readText() != File(pluginRoot, source).readText()
        }
        val findings = differing.map { (source, target) ->
            val file = File(root, target)
            val state = if (file.isFile) "отличается от стандарта" else "нет"
            if (mode.apply) {
                file.parentFile.mkdirs()
                file.writeText(File(pluginRoot, source).readText())
            }
            if (mode.apply) {
                val text = "$target записан ($state) — закоммитьте через задачу"
                Finding(status = Finding.Status.FIXED, area = AREA, text = text)
            } else {
                Finding(status = Finding.Status.DEVIATION, area = AREA, text = "$target: $state")
            }
        }
        val standardTemplates = pairs.map { (_, target) -> target }.toSet()
        val foreign = File(root, ISSUE_TEMPLATES).listFiles().orEmpty().map { file -> "$ISSUE_TEMPLATES/${file.name}" }
            .filter { path -> path !in standardTemplates }.sorted().map { path ->
                Finding(status = Finding.Status.NOTE, area = AREA, text = "шаблон вне стандарта: $path")
            }
        val ok = if (findings.isEmpty()) {
            listOf(Finding(status = Finding.Status.OK, area = AREA, text = "копии стандарта совпадают (${pairs.size})"))
        } else {
            emptyList()
        }
        return ok + findings + foreign
    }

    /** Пары «файл плагина → файл проекта». */
    private fun copies(): List<Pair<String, String>> {
        val templates = File(pluginRoot, SOURCE_TEMPLATES).listFiles().orEmpty().filter { file -> file.isFile }
            .sortedBy { file -> file.name }
            .map { file -> "$SOURCE_TEMPLATES/${file.name}" to "$ISSUE_TEMPLATES/${file.name}" }
        return templates + listOf(
            "templates/github/pull_request_template.md" to ".github/pull_request_template.md",
            "templates/project/editorconfig" to ".editorconfig",
        )
    }

    private companion object {
        const val AREA = "файлы"
        const val SOURCE_TEMPLATES = "templates/github/ISSUE_TEMPLATE"
        const val ISSUE_TEMPLATES = ".github/ISSUE_TEMPLATE"
    }
}

package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.git.Git
import java.io.File

private const val AREA = "python"
private const val PYTHON_FILES = "*.py"
private const val TEMPLATES = "templates/project"
private const val RUFF_STANDARD = "ruff-standard.toml"
private const val RUFF_CONFIG = "ruff.toml"
private const val RUFF_DOTFILE = ".ruff.toml"
private const val PYTEST_CONFIG = "pytest.ini"
private const val COVERAGE_CONFIG = ".coveragerc"
private const val PYPROJECT = "pyproject.toml"
internal const val EXTEND_LINE = "extend = \"$RUFF_STANDARD\""
private const val TOOL_RUFF = "[tool.ruff"
private const val TOOL_PYTEST = "[tool.pytest"
private const val TOOL_COVERAGE = "[tool.coverage"

// Копии стандарта: проект их не правит, поэтому setup перезаписывает их при расхождении.
private val copies = listOf(
    "$TEMPLATES/$RUFF_STANDARD" to RUFF_STANDARD,
    "$TEMPLATES/$PYTEST_CONFIG" to PYTEST_CONFIG,
)

// Заготовки проекта: в них настройки и порог покрытия проекта, поэтому setup создаёт их один раз и не трогает.
private val seeds = listOf(
    "$TEMPLATES/$RUFF_CONFIG" to RUFF_CONFIG,
    "$TEMPLATES/coveragerc" to COVERAGE_CONFIG,
)

/**
 * Конфиги проверок Python — только в проекте, где git отслеживает файлы `*.py`. Копии стандарта сверяет
 * [StandardCopies]; заготовки создаются, если их нет. Настройки ruff мимо `ruff.toml` — отклонение с советом: ruff
 * их не читает или читает вместо стандарта.
 */
class PythonConfig(
    private val git: Git,
    private val pluginRoot: File,
) : Setup.Step {

    private val standardCopies = StandardCopies(pluginRoot, AREA)

    override fun run(mode: Setup.Mode): List<Finding> {
        val pythonFiles = git.trackedPaths(PYTHON_FILES)
        if (pythonFiles.isEmpty()) {
            val text = "файлов Python нет — конфиги не нужны"
            return listOf(Finding(status = Finding.Status.OK, area = AREA, text = text))
        }
        val root = File(git.root())
        val copyFindings = standardCopies.run(root, copies, mode)
        val findings = copyFindings + createMissingSeeds(root, mode) + findConfigDeviations(root)
        return findings.ifEmpty {
            listOf(Finding(status = Finding.Status.OK, area = AREA, text = "конфиги Python по стандарту"))
        }
    }

    private fun createMissingSeeds(root: File, mode: Setup.Mode): List<Finding> {
        val missing = seeds.filter { (_, target) -> !File(root, target).isFile }
        return missing.map { (source, target) ->
            if (mode.apply) {
                File(root, target).writeText(File(pluginRoot, source).readText())
                val text = "$target создан по заготовке — дальше его ведёт проект; закоммитьте через задачу"
                Finding(status = Finding.Status.FIXED, area = AREA, text = text)
            } else {
                Finding(status = Finding.Status.DEVIATION, area = AREA, text = "$target: нет")
            }
        }
    }

    private fun findConfigDeviations(root: File): List<Finding> {
        val texts = mutableListOf<String>()
        val ruffConfig = File(root, RUFF_CONFIG)
        if (ruffConfig.isFile && EXTEND_LINE !in readTrimmedLines(ruffConfig)) {
            texts += "$RUFF_CONFIG: нет строки $EXTEND_LINE — стандарт не действует"
        }
        if (File(root, RUFF_DOTFILE).isFile) {
            texts += "$RUFF_DOTFILE: ruff читает его вместо $RUFF_CONFIG — перенесите настройки в $RUFF_CONFIG"
        }
        // Ruff, pytest и coverage берут первый найденный конфиг: рядом с ruff.toml, pytest.ini и .coveragerc
        // разделы pyproject.toml молча не действуют.
        val pyproject = readTrimmedLines(File(root, PYPROJECT))
        if (pyproject.any { line -> line.startsWith(prefix = TOOL_RUFF) }) {
            texts += "$PYPROJECT: [tool.ruff] не действует рядом с $RUFF_CONFIG — перенесите настройки в $RUFF_CONFIG"
        }
        if (pyproject.any { line -> line.startsWith(prefix = TOOL_PYTEST) }) {
            texts += "$PYPROJECT: [tool.pytest] не действует рядом с $PYTEST_CONFIG — " +
                "настройки pytest задаёт стандарт, уберите раздел"
        }
        if (pyproject.any { line -> line.startsWith(prefix = TOOL_COVERAGE) }) {
            texts += "$PYPROJECT: [tool.coverage] не действует рядом с $COVERAGE_CONFIG — " +
                "перенесите настройки в $COVERAGE_CONFIG"
        }
        return texts.map { text -> Finding(status = Finding.Status.DEVIATION, area = AREA, text = text) }
    }

    private fun readTrimmedLines(file: File): List<String> {
        if (!file.isFile) {
            return emptyList()
        }
        return file.readLines()
            .map { line -> line.trim() }
    }
}

package io.github.nakulov.standards.setup

import java.io.File

private const val AREA = "python"
const val PYTHON_PATTERN = "*.py"
const val STANDARD_RUFF = "стандарт ruff\n"
const val STANDARD_PYTEST = "[pytest]\n"
const val RUFF_SEED = "extend = \"ruff-standard.toml\"\n"
const val COVERAGE_SEED = "[report]\nfail_under = 0\n"
const val PROJECT_RUFF = "extend = \"ruff-standard.toml\"\nsrc = [\"tools\"]\n"

/** Корень плагина с шаблонами Python: две копии стандарта и две заготовки проекта. */
fun makePythonPluginRoot(): File {
    val plugin = makeProjectRoot()
    writeProjectFile(root = plugin, path = "templates/project/ruff-standard.toml", text = STANDARD_RUFF)
    writeProjectFile(root = plugin, path = "templates/project/pytest.ini", text = STANDARD_PYTEST)
    writeProjectFile(root = plugin, path = "templates/project/ruff.toml", text = RUFF_SEED)
    writeProjectFile(root = plugin, path = "templates/project/coveragerc", text = COVERAGE_SEED)
    return plugin
}

fun makePythonPaths(): List<String> {
    return listOf(element = "tools/check.py")
}

fun makeNoPythonFinding(): Finding {
    return Finding(status = Finding.Status.OK, area = AREA, text = "файлов Python нет — конфиги не нужны")
}

fun makeMissingPythonFindings(): List<Finding> {
    return listOf(
        makePythonDeviation(text = "ruff-standard.toml: нет"),
        makePythonDeviation(text = "pytest.ini: нет"),
        makePythonDeviation(text = "ruff.toml: нет"),
        makePythonDeviation(text = ".coveragerc: нет"),
    )
}

fun makeWrittenPythonFindings(): List<Finding> {
    return listOf(
        makeFixedPythonFinding(text = "ruff-standard.toml записан (нет) — закоммитьте через задачу"),
        makeFixedPythonFinding(text = "pytest.ini записан (нет) — закоммитьте через задачу"),
        makeFixedPythonFinding(
            text = "ruff.toml создан по заготовке — дальше его ведёт проект; закоммитьте через задачу",
        ),
        makeFixedPythonFinding(
            text = ".coveragerc создан по заготовке — дальше его ведёт проект; закоммитьте через задачу",
        ),
    )
}

fun makePythonFiles(): Map<String, String> {
    return makePythonFilesWithRuff(ruff = RUFF_SEED)
}

fun makePythonFilesWithProjectRuff(): Map<String, String> {
    return makePythonFilesWithRuff(ruff = PROJECT_RUFF)
}

fun makePythonDeviation(text: String): Finding {
    return Finding(status = Finding.Status.DEVIATION, area = AREA, text = text)
}

fun makePythonOk(): Finding {
    return Finding(status = Finding.Status.OK, area = AREA, text = "конфиги Python по стандарту")
}

private fun makeFixedPythonFinding(text: String): Finding {
    return Finding(status = Finding.Status.FIXED, area = AREA, text = text)
}

private fun makePythonFilesWithRuff(ruff: String): Map<String, String> {
    return mapOf(
        "ruff-standard.toml" to STANDARD_RUFF,
        "pytest.ini" to STANDARD_PYTEST,
        "ruff.toml" to ruff,
        ".coveragerc" to COVERAGE_SEED,
    )
}

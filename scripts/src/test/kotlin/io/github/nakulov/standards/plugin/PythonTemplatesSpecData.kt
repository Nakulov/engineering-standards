package io.github.nakulov.standards.plugin

const val TOOL_PATH = "tools/tool.py"
const val TEST_TOOL_PATH = "tests/test_tool.py"

/** Скрипт по стандарту: русские тексты, `print`, вызов `git`; тест покрывает только `greet`. */
const val TOOL_PY = "\"\"\"Скрипт-образец: русские тексты, print и вызов git.\"\"\"\n" +
    "\n" +
    "import subprocess\n" +
    "\n" +
    "\n" +
    "def branch() -> str:\n" +
    "    \"\"\"Возвращает текущую ветку git.\"\"\"\n" +
    "    command = [\"git\", \"branch\", \"--show-current\"]\n" +
    "    result = subprocess.run(command, capture_output=True, text=True, check=True)\n" +
    "    return result.stdout.strip()\n" +
    "\n" +
    "\n" +
    "def greet(name: str) -> str:\n" +
    "    \"\"\"Возвращает приветствие «Привет, <имя>».\"\"\"\n" +
    "    return f\"Привет, {name}\"\n" +
    "\n" +
    "\n" +
    "if __name__ == \"__main__\":\n" +
    "    print(greet(branch()))\n"

/** Тест без докстринга, с `assert` и литералом — по PY-TEST-3; скрипт — в `tools/` без `__init__.py` (PY-LINT-3). */
const val TEST_TOOL_PY = "from tools.tool import greet\n" +
    "\n" +
    "\n" +
    "def test_greet() -> None:\n" +
    "    assert greet(\"Аня\") == \"Привет, Аня\"\n"

const val UNUSED_IMPORT_PY = "\"\"\"Скрипт-образец с неиспользуемым импортом.\"\"\"\n" +
    "\n" +
    "import os\n"

/** Строка в 112 символов: длиннее 88 (умолчание ruff), но в пределах 120 (PY-FMT-1). */
fun makeLongLineProject(): Map<String, String> {
    return mapOf(TOOL_PATH to makeMessagePy(length = 100))
}

/** Строка в 131 символ: длиннее 120 — нарушение вне набора ruff по умолчанию. */
fun makeTooLongLineProject(): Map<String, String> {
    return mapOf(TOOL_PATH to makeMessagePy(length = 119))
}

private fun makeMessagePy(length: Int): String {
    return "\"\"\"Скрипт-образец с длинной строкой.\"\"\"\n\nMESSAGE = \"${"а".repeat(length)}\"\n"
}

/** Докстринг без точки: нарушение D415 по Google (PY-FMT-3); `D` вне набора ruff по умолчанию. */
const val NON_GOOGLE_DOC_PY = "\"\"\"Скрипт-образец с докстрингом не по Google.\"\"\"\n" +
    "\n" +
    "\n" +
    "def greet() -> None:\n" +
    "    \"\"\"Печатает приветствие\"\"\"\n"

/** Функция без аннотаций: нарушение `ANN` вне набора ruff по умолчанию. */
const val NO_ANNOTATION_PY = "\"\"\"Скрипт-образец без аннотаций.\"\"\"\n" +
    "\n" +
    "\n" +
    "def greet(name):\n" +
    "    \"\"\"Возвращает приветствие.\"\"\"\n" +
    "    return name\n"

fun makeNonGoogleDocProject(): Map<String, String> {
    return mapOf(TOOL_PATH to NON_GOOGLE_DOC_PY)
}

fun makeNoAnnotationProject(): Map<String, String> {
    return mapOf(TOOL_PATH to NO_ANNOTATION_PY)
}

fun makeCleanProject(): Map<String, String> {
    return mapOf(TOOL_PATH to TOOL_PY, TEST_TOOL_PATH to TEST_TOOL_PY)
}

fun makeUnusedImportProject(): Map<String, String> {
    return mapOf(TOOL_PATH to UNUSED_IMPORT_PY)
}

fun makeCleanCheckOutput(): List<String> {
    return listOf("0", "All checks passed!")
}

fun makeCleanFormatOutput(): List<String> {
    return listOf("0", "2 files already formatted")
}

fun makeUnusedImportOutput(): List<String> {
    return listOf(
        "1",
        "tools/tool.py:3:8: F401 [*] `os` imported but unused",
        "Found 1 error.",
        "[*] 1 fixable with `--fix`.",
    )
}

fun makeLongLineFormatOutput(): List<String> {
    return listOf("0", "1 file already formatted")
}

fun makeTooLongLineOutput(): List<String> {
    return listOf("1", "tools/tool.py:3:121: E501 Line too long (131 > 120)", "Found 1 error.")
}

fun makeNonGoogleDocOutput(): List<String> {
    return listOf(
        "1",
        "tools/tool.py:5:5: D415 First line should end with a period, question mark, or exclamation point",
        "Found 1 error.",
        "1 unsafe fix available with `--fix --unsafe-fixes`.",
    )
}

fun makeNoAnnotationOutput(): List<String> {
    return listOf(
        "1",
        "tools/tool.py:4:5: ANN201 Missing return type annotation for public function `greet`",
        "tools/tool.py:4:11: ANN001 Missing type annotation for function argument `name`",
        "Found 2 errors.",
    )
}

/** Порог 100% при покрытии 57%; блок `__main__` и `tests/` — вне подсчёта, `tools/` без `__init__.py` — в нём. */
fun makeLowCoverageProject(): Map<String, String> {
    return mapOf(TOOL_PATH to TOOL_PY, TEST_TOOL_PATH to TEST_TOOL_PY)
}

fun makeCoverageFailure(): List<String> {
    return listOf("1", "FAIL Required test coverage of 100.0% not reached. Total coverage: 57.14%")
}

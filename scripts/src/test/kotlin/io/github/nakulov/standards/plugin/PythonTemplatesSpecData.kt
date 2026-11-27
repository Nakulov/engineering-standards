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

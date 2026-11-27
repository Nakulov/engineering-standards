package io.github.nakulov.standards.plugin

import io.github.nakulov.standards.setup.EXTEND_LINE
import java.io.File

private const val TEMPLATE = "templates/project/ruff.toml"

// Исключение KT-TEST-6: читает настоящий шаблон — PythonConfigSpec работает с фальшивым корнем плагина и не видит,
// что строка extend, по которой PythonConfig проверяет ruff.toml проекта, есть в заготовке.
fun hasRuffSeedExtend(): Boolean {
    val lines = File(System.getProperty(/* key = */ "task.repoRoot"), TEMPLATE).readLines()
        .map { line -> line.trim() }
    return EXTEND_LINE in lines
}

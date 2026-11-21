package io.github.nakulov.standards.detekt

import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.test.lint
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.compileContentForTest
import dev.detekt.test.utils.createEnvironment

/** Находки правила на коде [code] из файла [filename]: строки «строка:столбец сообщение». */
internal fun Rule.lintCode(code: String, filename: String): List<String> {
    val file = compileContentForTest(content = code, filename = filename)
    return lint(file).toLines()
}

/** То же для правила с анализом типов: код компилируется с classpath тестов (JDK, stdlib). */
internal fun <T> T.lintCodeWithContext(code: String): List<String> where T : Rule, T : RequiresAnalysisApi {
    return lintWithContext(environment = createEnvironment(), content = code).toLines()
}

private fun List<Finding>.toLines(): List<String> {
    return map { finding ->
        val source = finding.entity.location.source
        "${source.line}:${source.column} ${finding.message}"
    }
}

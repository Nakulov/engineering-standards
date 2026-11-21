package io.github.nakulov.standards.detekt

import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment

// Вне стандарта (KT-TEST-6): находку detekt не сравнить с ожидаемым целиком — в ней PSI-элемент файла,
// поэтому тесты сравнивают строки «строка:столбец сообщение».
internal fun <T> T.findLintLines(code: String): List<String> where T : Rule, T : RequiresAnalysisApi {
    return lintWithContext(createEnvironment(), code).map { finding ->
        val source = finding.entity.location.source
        "${source.line}:${source.column} ${finding.message}"
    }
}

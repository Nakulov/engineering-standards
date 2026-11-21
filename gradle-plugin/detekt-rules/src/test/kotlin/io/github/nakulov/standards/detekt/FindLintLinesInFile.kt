package io.github.nakulov.standards.detekt

import dev.detekt.api.Rule
import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest

// Исключение KT-TEST-6: находку detekt не сравнить с ожидаемым целиком — в ней PSI-элемент файла,
// поэтому тесты сравнивают строки «строка:столбец сообщение».
internal fun Rule.findLintLinesInFile(code: String, filename: String): List<String> {
    val file = compileContentForTest(code, filename)
    return lint(file)
        .map { finding ->
            val source = finding.entity
                .location
                .source
            "${source.line}:${source.column} ${finding.message}"
        }
}

package io.github.nakulov.standards.gradle

import org.gradle.testkit.runner.GradleRunner
import java.io.File

private const val FIXTURES = "standards.fixtures"
private const val CHECK = "check"

// Исключение KT-TEST-6: вывод Gradle — сотни строк с абсолютным путём образца; тест сравнивает только строки
// с [mark] без пути. Образцы задач 9 и 10 используют её же: находка detekt, ошибка конфигурации, порог Kover.
internal fun findFailedCheckLines(fixture: String, mark: String): List<String> {
    val fixtures = System.getProperty(FIXTURES)
    val project = File(fixtures, fixture)
    val result = GradleRunner.create()
        .withProjectDir(project)
        .withPluginClasspath()
        .withArguments(CHECK)
        .buildAndFail()
    return result.output
        .lines()
        .filter { line -> mark in line }
        .map { line -> line.replace(project.absolutePath, newValue = "") }
}

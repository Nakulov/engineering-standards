package io.github.nakulov.standards.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

private const val FIXTURES = "standards.fixtures"
private const val CHECK = "check"
private const val CHECK_PATH = ":check"

// Исключение KT-TEST-6: вывод Gradle — сотни строк с абсолютным путём образца; тест сравнивает только строки
// с [mark] без пути. Образцы задач 9 и 10 используют её же: находка detekt, ошибка конфигурации, порог Kover.
internal fun findFailedCheckLines(fixture: String, mark: String): List<String> {
    val project = File(System.getProperty(FIXTURES), fixture)
    val result = checkRunner(project).buildAndFail()
    return result.output
        .lines()
        .filter { line -> mark in line }
        .map { line -> line.replace(project.absolutePath, newValue = "") }
}

// Чистый образец: build() роняет тест с выводом Gradle, если check упал.
internal fun findCheckOutcome(fixture: String): TaskOutcome? {
    val project = File(System.getProperty(FIXTURES), fixture)
    return checkRunner(project).build().task(CHECK_PATH)?.outcome
}

private fun checkRunner(project: File): GradleRunner {
    return GradleRunner.create()
        .withProjectDir(project)
        .withPluginClasspath()
        .withArguments(CHECK)
}

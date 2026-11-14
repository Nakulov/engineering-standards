import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
}

// Логику компилирует и `kotlinc` человека при первом запуске `scripts/task`: минимальная версия языка и API задаётся
// здесь, чтобы код новее неё не прошёл сборку (#13, решение 1); то же число — в `scripts/task`. Тесты запускаются только Gradle, им ограничение не нужно.
tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    testImplementation("io.github.nakulov:kotestkit:2.2.0")
    testImplementation("io.mockk:mockk:1.14.9")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("task.launcher", file("task").absolutePath)
    systemProperty("task.repoRoot", rootProject.projectDir.absolutePath)
    systemProperty("task.prTemplate", rootProject.file("templates/github/pull_request_template.md").absolutePath)
    systemProperty("task.cache", layout.buildDirectory.dir("test-cache").get().asFile.absolutePath)
}

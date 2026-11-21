import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

// Kotlin DSL проекта читает классы плагина компилятором Gradle: метаданные — не новее Kotlin внутри Gradle.
kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

// Плагин Kotlin для проекта-образца TestKit: на одном classpath с плагином, иначе detekt не видит классы Kotlin.
val testPluginClasspath: Configuration by configurations.creating

dependencies {
    implementation(project(":detekt-rules"))
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.kover.gradle.plugin)

    testImplementation(libs.kotestkit)
    testPluginClasspath(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("standardsKotlin") {
            id = "standards.kotlin"
            implementationClass = "io.github.nakulov.standards.gradle.StandardsKotlinPlugin"
        }
    }
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(testPluginClasspath)
}

// Проекты-образцы TestKit: путь — тестам, содержимое — входы теста (без их build и кэшей).
tasks.test {
    useJUnitPlatform()
    val fixtures = file("src/test/fixtures")
    systemProperty("standards.fixtures", fixtures.absolutePath)
    val fixtureFiles = fileTree(fixtures) {
        exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
    }
    inputs.files(fixtureFiles)
}

package io.github.nakulov.standards.gradle

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.net.JarURLConnection

private const val PLUGIN_ID = "standards.kotlin"
private const val KOTLIN_PLUGIN = "org.jetbrains.kotlin.jvm"
private const val DETEKT_PLUGIN = "dev.detekt"
private const val DETEKT_PLUGINS = "detektPlugins"
private const val KTLINT_WRAPPER = "dev.detekt:detekt-rules-ktlint-wrapper"
private const val CONFIG_RESOURCE = "/standards/detekt.yml"
private const val RULES_CLASS = "io/github/nakulov/standards/detekt/StandardsProvider.class"
private const val BASELINE = "detekt-baseline.xml"
private const val CHECK = "check"
private const val PLAIN_DETEKT = "detekt"
private const val DETEKT_MAIN = "detektMain"
private const val DETEKT_TEST = "detektTest"

/**
 * Плагин соглашений Kotlin `standards.kotlin`: detekt с конфигом стандарта, своими правилами и ktlint.
 * Kotlin не применяет — версия, toolchain и `languageVersion` остаются в проекте.
 */
class StandardsKotlinPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager
            .withPlugin(KOTLIN_PLUGIN) { _ -> applyDetekt(project) }
    }

    private fun applyDetekt(project: Project) {
        project.pluginManager
            .apply(DETEKT_PLUGIN)
        val detekt = project.extensions
            .getByType(DetektExtension::class.java)
        detekt.buildUponDefaultConfig
            .set(/* value = */ true)
        val config = project.provider {
            project.resources
                .text
                .fromString(getConfigText())
                .asFile()
        }
        detekt.config
            .from(config)
        // Бейзлайн проекта: detekt берёт detekt-baseline-main.xml / -test.xml, отсутствующий файл пропускает.
        val baseline = project.file(BASELINE)
        detekt.baseline
            .set(baseline)
        val rules = project.files(findRulesJar())
        project.dependencies
            .add(DETEKT_PLUGINS, rules)
        // Версия ktlint-обёртки — версия самого detekt: одно значение, без второй записи версии.
        val ktlintWrapper = detekt.toolVersion
            .map { version -> "$KTLINT_WRAPPER:$version" }
        project.dependencies
            .addProvider(DETEKT_PLUGINS, ktlintWrapper)
        // NamedArguments и NamedLiteralArgument работают только с анализом типов: check — detektMain и detektTest.
        project.tasks
            .named(CHECK) { check -> dependOnTypedDetekt(check) }
    }

    private fun dependOnTypedDetekt(check: Task) {
        val dependencies = check.dependsOn
            .filterNot { dependency -> (dependency as? TaskProvider<*>)?.name == PLAIN_DETEKT }
        check.setDependsOn(dependencies)
        check.dependsOn(DETEKT_MAIN, DETEKT_TEST)
    }

    private fun getConfigText(): String {
        val resource = javaClass.getResource(CONFIG_RESOURCE)
        requireNotNull(resource) { "$PLUGIN_ID: в плагине нет $CONFIG_RESOURCE" }
        return resource.readText()
    }

    // Свои правила — jar модуля detekt-rules с classpath плагина: так они находятся и из includeBuild, и в TestKit.
    private fun findRulesJar(): File {
        val resource = javaClass.classLoader
            .getResource(RULES_CLASS)
        requireNotNull(resource) { "$PLUGIN_ID: правил detekt стандарта нет на classpath плагина" }
        val connection = resource.openConnection()
        require(connection is JarURLConnection) { "$PLUGIN_ID: правила detekt не в jar: $resource" }
        val jar = connection.jarFileURL
            .toURI()
        return File(jar)
    }
}

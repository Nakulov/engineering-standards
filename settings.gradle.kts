pluginManagement {
    // Плагин соглашений стандарта standards.kotlin и свои правила detekt — сборка gradle-plugin/.
    includeBuild("gradle-plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    // Версия строкой, а не из gradle/libs.versions.toml: в plugins {} файла settings каталог ещё недоступен.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()

        maven {
            url = uri("https://maven.pkg.github.com/Nakulov/KotestKit")
            credentials {
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.key").get()
            }
        }
    }
}

rootProject.name = "engineering-standards"
include("scripts")

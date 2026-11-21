// Общая сборка стандарта: свои правила detekt и плагин соглашений. Отдельная сборка, а не buildSrc,
// чтобы эпик доставки опубликовал её без переписывания; проекты подключают её через pluginManagement.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.4.21"
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()

        // KotestKit нужен только тестам сборки: без учётных данных сборка плагина для проекта не падает.
        maven {
            url = uri("https://maven.pkg.github.com/Nakulov/KotestKit")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}

rootProject.name = "gradle-plugin"
include("detekt-rules")

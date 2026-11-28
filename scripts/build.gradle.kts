plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    id("standards.kotlin")
    `maven-publish`
}

val versionScript = file(/* path = */ "task-version")
val testPackages = layout.buildDirectory.dir(/* path = */ "test-packages")

// Версия jar — вывод scripts/task-version: под ней CI публикует jar, по ней запускатель ищет его в кеше и GitHub
// Packages.
group = "io.github.nakulov.standards"
version = providers.exec { commandLine(versionScript.absolutePath) }
    .standardOutput
    .asText
    .get()
    .trim()

dependencies {
    testImplementation(libs.kotestkit)
    testImplementation(libs.mockk)
}

// Один jar с библиотеками рабочего кода: запускатель запускает его `java -cp` без Gradle. Имя без версии — по нему
// запускатель забирает локальную сборку.
tasks.shadowJar {
    archiveBaseName = "task"
    archiveClassifier = ""
    archiveVersion = ""
}

// gitHubPackages — публикация из CI (.github/workflows/publish-task.yml); testPackages — тот же jar в папке сборки,
// из неё скачивает TaskLauncherSpec.
publishing {
    publications {
        create<MavenPublication>(/* name = */ "task") {
            artifactId = "task"
            from(components["shadow"])
        }
    }
    repositories {
        maven {
            name = "gitHubPackages"
            url = uri(/* path = */ "https://maven.pkg.github.com/Nakulov/engineering-standards")
            credentials {
                username = providers.gradleProperty(/* propertyName = */ "gpr.user").get()
                password = providers.gradleProperty(/* propertyName = */ "gpr.key").get()
            }
        }
        maven {
            name = "testPackages"
            url = uri(testPackages)
        }
    }
}

// Порог покрытия — замер scripts/ с округлением вниз (KT-TEST-7); поднимает #107, не снижаем.
standards {
    coverageMinBound = 97
}

// Точки входа: exitProcess завершает JVM, в тестах их не вызвать (KT-TEST-7); проверяются запуском scripts/task и scripts/setup.
kover {
    reports {
        filters {
            excludes {
                classes("io.github.nakulov.standards.task.cli.MainKt", "io.github.nakulov.standards.setup.SetupMainKt")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // Спека запускателя скачивает jar из testPackages и забирает локальную сборку build/libs/task.jar.
    dependsOn("publishTaskPublicationToTestPackagesRepository")
    systemProperty("task.packages", testPackages.get().asFile.absolutePath)
    systemProperty("task.jar", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("task.launcher", file("task").absolutePath)
    systemProperty("task.setupLauncher", file("setup").absolutePath)
    systemProperty("task.versionScript", versionScript.absolutePath)
    systemProperty("task.version", version.toString())
    systemProperty("task.repoRoot", rootProject.projectDir.absolutePath)
    systemProperty("task.prTemplate", rootProject.file("templates/github/pull_request_template.md").absolutePath)
    systemProperty("task.cache", layout.buildDirectory.dir("test-cache").get().asFile.absolutePath)
    // Спеки читают эти файлы через task.repoRoot: без входов правка только в них оставляет тест UP-TO-DATE.
    // Запускатели — входы спеки запускателей: их правка тоже должна перезапускать тест.
    inputs.files(
        rootProject.files(
            "skills", "agents", "rules", "docs", "templates", ".github",
            "hooks/session-start", "scripts/task-wrapper", "scripts/task", "scripts/setup", "scripts/task-version",
        ),
    )
        .withPropertyName(/* propertyName = */ "repoFiles")
}

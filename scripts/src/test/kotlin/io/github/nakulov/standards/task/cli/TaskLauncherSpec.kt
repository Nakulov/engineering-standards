package io.github.nakulov.standards.task.cli

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.github.nakulov.standards.plugin.MISSING_DIRECTORY
import io.github.nakulov.standards.plugin.STANDARD_COPY
import io.github.nakulov.standards.plugin.makeCopyRun
import io.github.nakulov.standards.plugin.makeGitProject
import io.github.nakulov.standards.plugin.makeStandardCopy
import io.github.nakulov.standards.plugin.makeStandardRepo
import io.kotest.core.spec.style.BehaviorSpec
import kotlin.time.Duration.Companion.seconds

// Вне стандарта: спека проверяет не вызов класса, а запускатели `scripts/task`, `scripts/setup` и
// `scripts/task-version` — запускает их процессом с разным окружением: PATH с заглушкой java, папка пакетов, заглушка
// Gradle, кеш. Окружение готовится файлами (KT-TEST-6): скрипт bash за интерфейс не спрятать. Запуск и окружения — в
// TaskLauncherSpecData.
class TaskLauncherSpec :
    BehaviorSpec({
        shouldReturnTest(makeVersionRun(), options = TestOptions(name = "task-version: версия сборки Gradle")) {
            runLauncher(environment = makeBareEnvironment(), args = emptyList(), launcher = makeVersionScript())
        }

        shouldReturnTest(makeUnknownCommandRun(), options = TestOptions(name = "task: jar из GitHub Packages")) {
            runLauncher(environment = makeDownloadEnvironment(), args = listOf(UNKNOWN_COMMAND))
        }

        shouldReturnTest(makeUnknownCommandRun(), options = TestOptions(name = "task: jar из кеша")) {
            runLauncher(environment = makeCachedEnvironment(), args = listOf(UNKNOWN_COMMAND))
        }

        shouldReturnTest(makeLocalBuildRun(), options = TestOptions(name = "task: jar не скачан — локальная сборка")) {
            runLauncher(environment = makeLocalBuildEnvironment(), args = listOf(UNKNOWN_COMMAND))
        }

        shouldReturnTest(makeFailedBuildRun(), options = TestOptions(name = "task: jar не скачан, сборка упала")) {
            runLauncher(environment = makeFailedBuildEnvironment(), args = listOf(UNKNOWN_COMMAND))
        }

        shouldReturnTest(
            makeSetupHelpRun(),
            options = TestOptions(name = "setup --help: тот же jar, другая точка входа"),
        ) {
            runLauncher(
                environment = makeDownloadEnvironment(),
                args = listOf("--help"),
                launcher = makeSetupLauncher(),
            )
        }

        withGivens(
            nameFn = { given -> "без перенаправления, нет java: ${given.arg1}" },
            case(arg1 = listOf("--help")),
            case(arg1 = listOf("-C", makeRepoRoot(), "--help")),
            case(arg1 = listOf("-C", makeGitProject().path, "--help")),
            case(arg1 = listOf("-C", MISSING_DIRECTORY, "--help")),
        ) { args ->
            shouldReturnTest(makeNoJavaRun()) {
                runLauncher(environment = makeNoJavaEnvironment(), args = args, timeout = 10.seconds)
            }
        }

        shouldReturnTest(makeCopyRun(), options = TestOptions(name = "-C в копию стандарта — task копии")) {
            val repo = makeStandardRepo()
            makeStandardCopy(repo = repo)
            runLauncher(
                environment = makeBareEnvironment(),
                args = listOf("-C", STANDARD_COPY, "--help"),
                directory = repo,
            )
        }

        shouldReturnTest(makeOldJavaRun(), options = TestOptions(name = "java 17")) {
            runLauncher(environment = makeOldJavaEnvironment(), args = listOf("--help"))
        }
    })

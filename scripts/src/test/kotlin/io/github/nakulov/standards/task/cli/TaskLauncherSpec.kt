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
        withGivens(
            nameFn = { case -> "запуск: ${case.arg1}" },
            case(
                arg1 = "task-version: версия сборки Gradle",
                arg2 = makeVersionRun(),
                arg3 = { makeBareEnvironment() },
                arg4 = emptyList(),
                arg5 = makeVersionScript(),
            ),
            case(
                arg1 = "task: jar из GitHub Packages",
                arg2 = makeUnknownCommandRun(),
                arg3 = { makeDownloadEnvironment() },
                arg4 = listOf(UNKNOWN_COMMAND),
                arg5 = makeTaskLauncher(),
            ),
            case(
                arg1 = "task: jar из кеша",
                arg2 = makeUnknownCommandRun(),
                arg3 = { makeCachedEnvironment() },
                arg4 = listOf(UNKNOWN_COMMAND),
                arg5 = makeTaskLauncher(),
            ),
            case(
                arg1 = "task: jar не скачан — локальная сборка",
                arg2 = makeLocalBuildRun(),
                arg3 = { makeLocalBuildEnvironment() },
                arg4 = listOf(UNKNOWN_COMMAND),
                arg5 = makeTaskLauncher(),
            ),
            case(
                arg1 = "task: jar не скачан, сборка упала",
                arg2 = makeFailedBuildRun(),
                arg3 = { makeFailedBuildEnvironment() },
                arg4 = listOf(UNKNOWN_COMMAND),
                arg5 = makeTaskLauncher(),
            ),
            case(
                arg1 = "setup --help: тот же jar, другая точка входа",
                arg2 = makeSetupHelpRun(),
                arg3 = { makeDownloadEnvironment() },
                arg4 = listOf("--help"),
                arg5 = makeSetupLauncher(),
            ),
            case(
                arg1 = "java 17",
                arg2 = makeOldJavaRun(),
                arg3 = { makeOldJavaEnvironment() },
                arg4 = listOf("--help"),
                arg5 = makeTaskLauncher(),
            ),
        ) { name, expected, environment, args, launcher ->
            shouldReturnTest(expected, options = TestOptions(name)) {
                runLauncher(environment(), args, launcher)
            }
        }

        withGivens(
            nameFn = { given -> "без перенаправления, нет java: ${given.arg1}" },
            case(arg1 = listOf("--help")),
            case(arg1 = listOf("-C", makeRepoRoot(), "--help")),
            case(arg1 = listOf("-C", makeGitProject().path, "--help")),
            case(arg1 = listOf("-C", MISSING_DIRECTORY, "--help")),
        ) { args ->
            shouldReturnTest(makeNoJavaRun()) {
                runLauncher(makeNoJavaEnvironment(), args, timeout = 10.seconds)
            }
        }

        shouldReturnTest(makeCopyRun(), options = TestOptions(name = "-C в копию стандарта — task копии")) {
            val repo = makeStandardRepo()
            makeStandardCopy(repo = repo)
            runLauncher(makeBareEnvironment(), listOf("-C", STANDARD_COPY, "--help"), directory = repo)
        }
    })

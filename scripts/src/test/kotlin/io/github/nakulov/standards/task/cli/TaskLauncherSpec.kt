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
import io.kotest.core.test.config.TestConfig
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Вне стандарта: спека проверяет не вызов класса, а запускатель `scripts/task` — запускает его процессом с разным PATH
// и `-C` (#13, решения 1, 3 и 7; #134) — и запускатель `scripts/setup` поверх него (#8). Запуск и подготовка PATH — в
// TaskLauncherSpecData.
class TaskLauncherSpec :
    BehaviorSpec({
        shouldReturnTest(makeVersionRun(), options = TestOptions(name = "task-version: версия сборки Gradle")) {
            runLauncher(path = makeBarePath(), args = emptyList(), launcher = makeVersionScript())
        }

        shouldReturnTest(
            makeUnknownCommandRun(),
            options = TestOptions(
                name = "task: компиляция в кеш и запуск",
                config = TestConfig(timeout = 3.minutes),
            ),
        ) {
            runLauncher(path = makeSystemPath(), args = listOf(UNKNOWN_COMMAND))
        }

        shouldReturnTest(
            makeSetupHelpRun(),
            options = TestOptions(
                name = "setup --help: та же компиляция, другая точка входа",
                config = TestConfig(timeout = 3.minutes),
            ),
        ) {
            runLauncher(path = makeSystemPath(), args = listOf("--help"), launcher = makeSetupLauncher())
        }

        withGivens(
            nameFn = { given -> "без перенаправления, нет kotlinc в PATH: ${given.arg1}" },
            case(arg1 = listOf("--help")),
            case(arg1 = listOf("-C", makeRepoRoot(), "--help")),
            case(arg1 = listOf("-C", makeGitProject().path, "--help")),
            case(arg1 = listOf("-C", MISSING_DIRECTORY, "--help")),
        ) { args ->
            shouldReturnTest(makeNoKotlinRun()) {
                runLauncher(path = makeBarePath(), args = args, timeout = 10.seconds)
            }
        }

        shouldReturnTest(makeCopyRun(), options = TestOptions(name = "-C в копию стандарта — task копии")) {
            val repo = makeStandardRepo()
            makeStandardCopy(repo = repo)
            runLauncher(path = makeBarePath(), args = listOf("-C", STANDARD_COPY, "--help"), directory = repo)
        }

        shouldReturnTest(makeOldKotlinRun(), options = TestOptions(name = "kotlinc 2.1.20")) {
            runLauncher(path = makeOldKotlinPath(), args = listOf("--help"))
        }
    })

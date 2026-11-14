package io.github.nakulov.standards.task.cli

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.config.TestConfig
import kotlin.time.Duration.Companion.minutes

// Вне стандарта: спека проверяет не вызов класса, а запускатель `scripts/task` — запускает его процессом с разным PATH
// (#13, решения 1, 3 и 7). Запуск и подготовка PATH — в TaskLauncherSpecData.
class TaskLauncherSpec :
    BehaviorSpec({
        shouldReturnTest(
            makeHelpRun(),
            options = TestOptions(
                name = "task --help: компиляция в кеш и запуск",
                config = TestConfig(timeout = 3.minutes),
            ),
        ) {
            runLauncher(path = makeSystemPath(), args = listOf("--help"))
        }

        shouldReturnTest(makeNoKotlinRun(), options = TestOptions(name = "нет kotlinc в PATH")) {
            runLauncher(path = makeBarePath(), args = listOf("--help"))
        }

        shouldReturnTest(makeOldKotlinRun(), options = TestOptions(name = "kotlinc 2.1.20")) {
            runLauncher(path = makeOldKotlinPath(), args = listOf("--help"))
        }
    })

package io.github.nakulov.standards.plugin

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а шаблоны конфигов Python `templates/project/` на образцах проектов:
// ошибка в шаблоне ломает проверки во всех проектах. Сборка образца и запуск — в FindRuffOutput и FindPytestFailure.
class PythonTemplatesSpec : BehaviorSpec({
    shouldReturnTest(makeCleanCheckOutput(), options = TestOptions(name = "ruff check: образец по стандарту")) {
        findRuffOutput(files = makeCleanProject(), args = listOf("check", "--output-format", "concise"))
    }

    shouldReturnTest(makeCleanFormatOutput(), options = TestOptions(name = "ruff format: образец отформатирован")) {
        findRuffOutput(files = makeCleanProject(), args = listOf("format", "--check"))
    }

    shouldReturnTest(makeUnusedImportOutput(), options = TestOptions(name = "ruff check: неиспользуемый импорт")) {
        findRuffOutput(files = makeUnusedImportProject(), args = listOf("check", "--output-format", "concise"))
    }

    shouldReturnTest(makeCoverageFailure(), options = TestOptions(name = "pytest: покрытие ниже порога")) {
        findPytestFailure(files = makeLowCoverageProject())
    }
})

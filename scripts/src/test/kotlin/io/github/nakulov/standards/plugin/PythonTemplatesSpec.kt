package io.github.nakulov.standards.plugin

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а шаблоны конфигов Python `templates/project/` на образцах проектов:
// ошибка в шаблоне ломает проверки во всех проектах. Сборка образца и запуск — в FindRuffOutput и FindPytestFailure.
class PythonTemplatesSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> "ruff ${case.arg3.joinToString(separator = " ")}" },
        case(
            arg1 = "ruff check: образец по стандарту",
            arg2 = makeCleanProject(),
            arg3 = listOf("check", "--output-format", "concise"),
            arg4 = makeCleanCheckOutput(),
        ),
        case(
            arg1 = "ruff format: образец отформатирован",
            arg2 = makeCleanProject(),
            arg3 = listOf("format", "--check"),
            arg4 = makeCleanFormatOutput(),
        ),
        case(
            arg1 = "ruff check: неиспользуемый импорт",
            arg2 = makeUnusedImportProject(),
            arg3 = listOf("check", "--output-format", "concise"),
            arg4 = makeUnusedImportOutput(),
        ),
        case(
            arg1 = "ruff check: строка 112 символов — в пределах 120",
            arg2 = makeLongLineProject(),
            arg3 = listOf("check", "--output-format", "concise"),
            arg4 = makeCleanCheckOutput(),
        ),
        case(
            arg1 = "ruff format: строка 112 символов не переносится",
            arg2 = makeLongLineProject(),
            arg3 = listOf("format", "--check"),
            arg4 = makeLongLineFormatOutput(),
        ),
        case(
            arg1 = "ruff check: строка длиннее 120 символов",
            arg2 = makeTooLongLineProject(),
            arg3 = listOf("check", "--output-format", "concise"),
            arg4 = makeTooLongLineOutput(),
        ),
        case(
            arg1 = "ruff check: докстринг не по Google",
            arg2 = makeNonGoogleDocProject(),
            arg3 = listOf("check", "--output-format", "concise"),
            arg4 = makeNonGoogleDocOutput(),
        ),
        case(
            arg1 = "ruff check: функция без аннотаций",
            arg2 = makeNoAnnotationProject(),
            arg3 = listOf("check", "--output-format", "concise"),
            arg4 = makeNoAnnotationOutput(),
        ),
    ) { name, files, args, expected ->
        shouldReturnTest(expected, options = TestOptions(name)) {
            findRuffOutput(files, args)
        }
    }

    shouldReturnTest(makeCoverageFailure(), options = TestOptions(name = "pytest: покрытие ниже порога")) {
        findPytestFailure(files = makeLowCoverageProject())
    }

    shouldReturnTest(expected = true, options = TestOptions(name = "ruff.toml шаблона: строка extend")) {
        hasRuffSeedExtend()
    }
})

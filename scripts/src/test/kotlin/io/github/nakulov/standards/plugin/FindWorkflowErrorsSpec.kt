package io.github.nakulov.standards.plugin

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class FindWorkflowErrorsSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> "памятка: «${case.arg2}», скиллы: ${case.arg3}" },
        case(
            arg1 = "все скиллы упомянуты",
            arg2 = WORKFLOW_WITH_ALL_SKILLS,
            arg3 = makeSkills(),
            arg4 = emptyList<String>(),
        ),
        case(
            arg1 = "команда без скилла",
            arg2 = WORKFLOW_WITH_UNKNOWN_COMMAND,
            arg3 = makeSkills(),
            arg4 = makeUnknownCommandErrors(),
        ),
        case(
            arg1 = "скилл без команды",
            arg2 = WORKFLOW_WITHOUT_DECOMPOSE,
            arg3 = makeSkills(),
            arg4 = makeMissingDecomposeErrors(),
        ),
        case(
            arg1 = "повтор команды — одна ошибка",
            arg2 = WORKFLOW_WITH_REPEATED_UNKNOWN_COMMAND,
            arg3 = makeSkills(),
            arg4 = makeUnknownCommandErrors(),
        ),
        case(
            arg1 = "команда task — не скилл",
            arg2 = WORKFLOW_WITH_TASK_COMMAND,
            arg3 = makeSkills(),
            arg4 = emptyList<String>(),
        ),
        case(
            arg1 = "путь — не команда",
            arg2 = WORKFLOW_WITH_PATHS,
            arg3 = makeSkills(),
            arg4 = emptyList<String>(),
        ),
        case(
            arg1 = "команда без скилла и скилл без команды — сначала команды",
            arg2 = WORKFLOW_WITH_UNKNOWN_COMMAND_WITHOUT_DECOMPOSE,
            arg3 = makeSkills(),
            arg4 = makeMixedErrors(),
        ),
        case(
            arg1 = "пустая памятка — все скиллы без команды",
            arg2 = EMPTY_WORKFLOW,
            arg3 = makeSkills(),
            arg4 = makeAllSkillsMissingErrors(),
        ),
        case(
            arg1 = "скиллов нет — все команды без скилла",
            arg2 = WORKFLOW_WITH_ALL_SKILLS,
            arg3 = emptyList<String>(),
            arg4 = makeAllCommandsUnknownErrors(),
        ),
    ) { name, workflow, skills, expected ->
        shouldReturnTest(expected, options = TestOptions(name = name)) {
            findWorkflowErrors(workflow = workflow, skills = skills)
        }
    }

    // Вне стандарта: тест проверяет не функцию, а файлы репозитория — памятку `docs/workflow.md` и папку `skills/`:
    // переименованный или новый скилл без строки в памятке иначе молча расходится с ней.
    shouldReturnTest(emptyList<String>(), options = TestOptions(name = "памятка репозитория без ошибок")) {
        findRepoWorkflowErrors()
    }
})

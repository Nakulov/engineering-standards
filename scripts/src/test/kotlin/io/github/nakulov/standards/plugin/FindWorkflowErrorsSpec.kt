package io.github.nakulov.standards.plugin

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

class FindWorkflowErrorsSpec : BehaviorSpec({
    shouldReturnTest(emptyList<String>(), options = TestOptions(name = "все скиллы упомянуты")) {
        findWorkflowErrors(workflow = WORKFLOW_WITH_ALL_SKILLS, skills = makeSkills())
    }

    shouldReturnTest(makeUnknownCommandErrors(), options = TestOptions(name = "команда без скилла")) {
        findWorkflowErrors(workflow = WORKFLOW_WITH_UNKNOWN_COMMAND, skills = makeSkills())
    }

    shouldReturnTest(makeMissingDecomposeErrors(), options = TestOptions(name = "скилл без команды")) {
        findWorkflowErrors(workflow = WORKFLOW_WITHOUT_DECOMPOSE, skills = makeSkills())
    }

    shouldReturnTest(makeUnknownCommandErrors(), options = TestOptions(name = "повтор команды — одна ошибка")) {
        findWorkflowErrors(workflow = WORKFLOW_WITH_REPEATED_UNKNOWN_COMMAND, skills = makeSkills())
    }

    shouldReturnTest(emptyList<String>(), options = TestOptions(name = "команда task — не скилл")) {
        findWorkflowErrors(workflow = WORKFLOW_WITH_TASK_COMMAND, skills = makeSkills())
    }
})

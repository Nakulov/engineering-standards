package io.github.nakulov.standards.task.github

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class IssueTemplateSpec :
    BehaviorSpec({
        // Настоящие шаблоны `.github/ISSUE_TEMPLATE/`: у каждого типа — своя метка (#6, решение 1).
        withGivens(
            nameFn = { given -> "шаблон ${given.arg2}" },
            case(arg1 = "type:task", arg2 = "task"),
            case(arg1 = "type:bug", arg2 = "bug"),
            case(arg1 = "type:epic", arg2 = "epic"),
            case(arg1 = "type:research", arg2 = "research"),
        ) { expected, type ->
            shouldReturnTest(expected) {
                IssueTemplate.typeLabel(text = makeIssueTemplate(type = type), file = "$type.md")
            }
        }

        shouldReturnTest("type:task") {
            IssueTemplate.typeLabel(text = makeTwoLabelsTemplate(), file = "task.md")
        }

        withGivens(
            nameFn = { given -> "без метки типа: ${given.arg1.lines().first()}" },
            case(arg1 = "---\nname: Задача\nlabels: [process]\n---\n"),
            case(arg1 = "---\nname: Задача\n---\nlabels: [type:task]\n"),
            case(arg1 = "name: Задача\nlabels: [type:task]\n"),
        ) { text ->
            shouldThrowWithMessageTest<TaskError.Failure>(makeNoTypeLabelMessage()) {
                IssueTemplate.typeLabel(text = text, file = "task.md")
            }
        }
    })

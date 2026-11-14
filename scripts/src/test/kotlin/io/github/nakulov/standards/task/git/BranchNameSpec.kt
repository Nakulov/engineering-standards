package io.github.nakulov.standards.task.git

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class BranchNameSpec :
    BehaviorSpec({
        shouldReturnTest("task/13-task-scripts") {
            BranchName.of(issue = 13, slug = "task-scripts", long = false).toString()
        }

        shouldReturnTest("long/13-task-scripts") {
            BranchName.of(issue = 13, slug = "task-scripts", long = true).toString()
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeBadSlugMessage()) {
            BranchName.of(issue = 13, slug = "Скрипты", long = false)
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeLongSlugMessage()) {
            BranchName.of(issue = 13, slug = makeLongSlug(), long = false)
        }

        withGivens(
            nameFn = { given -> "ветка ${given.arg2}" },
            case(arg1 = makeTaskBranch(), arg2 = "task/13-task-scripts"),
            case(arg1 = makeLongBranch(), arg2 = "long/13-task-scripts"),
            case(arg1 = null, arg2 = "dev"),
            case(arg1 = null, arg2 = "feature/13-x"),
        ) { expected, branch ->
            shouldReturnTest(expected) { BranchName.parse(branch = branch) }
        }
    })

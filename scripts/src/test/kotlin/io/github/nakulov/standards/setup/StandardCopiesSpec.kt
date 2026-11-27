package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class StandardCopiesSpec : BehaviorSpec({
    val step = StandardCopies(makeCopiesPluginRoot(), COPIES_AREA)

    withGivens(
        nameFn = { case -> case.arg1 },
        case(
            arg1 = "копии нет — проверка",
            arg2 = makeEmptyProject(),
            arg3 = makeCheckMode(),
            arg4 = makeMissingCopyFindings(),
        ),
        case(
            arg1 = "копия отличается — проверка",
            arg2 = makeProjectWithOldCopy(),
            arg3 = makeCheckMode(),
            arg4 = makeDifferingCopyFindings(),
        ),
        case(
            arg1 = "копия совпадает — находок нет",
            arg2 = makeProjectWithStandardCopy(),
            arg3 = makeCheckMode(),
            arg4 = emptyList<Finding>(),
        ),
        case(
            arg1 = "копии нет — запись",
            arg2 = makeEmptyProject(),
            arg3 = makeApplyMode(),
            arg4 = makeWrittenMissingCopyFindings(),
        ),
        case(
            arg1 = "копия отличается — запись",
            arg2 = makeProjectWithOldCopy(),
            arg3 = makeApplyMode(),
            arg4 = makeWrittenDifferingCopyFindings(),
        ),
    ) { _, project, mode, expected ->
        shouldReturnTest(expected) {
            step.run(project, makeFlatCopies(), mode)
        }
    }

    withGivens(
        nameFn = { case -> case.arg1 },
        case(
            arg1 = "копии нет — файл записан",
            arg2 = makeEmptyProject(),
            arg3 = makeFlatCopies(),
            arg4 = makeFlatFiles(),
        ),
        case(
            arg1 = "копия отличается — файл перезаписан",
            arg2 = makeProjectWithOldCopy(),
            arg3 = makeFlatCopies(),
            arg4 = makeFlatFiles(),
        ),
        case(
            arg1 = "вложенная папка — создана",
            arg2 = makeEmptyProject(),
            arg3 = makeNestedCopies(),
            arg4 = makeNestedFiles(),
        ),
    ) { _, project, copies, expected ->
        shouldReturnTest(expected) {
            step.run(project, copies, makeApplyMode())
            readProjectFiles(project)
        }
    }
})

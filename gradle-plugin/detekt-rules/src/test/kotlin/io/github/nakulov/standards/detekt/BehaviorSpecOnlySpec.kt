package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class BehaviorSpecOnlySpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> case.arg2 },
        case(arg1 = makeFunSpecFindings(), arg2 = FUN_SPEC_CODE),
        case(arg1 = emptyList(), arg2 = BEHAVIOR_SPEC_CODE),
    ) { expected, code ->
        shouldReturnTest(expected) {
            BehaviorSpecOnly(Config.empty).findLintLinesInFile(code, filename = "ExampleSpec.kt")
        }
    }
})

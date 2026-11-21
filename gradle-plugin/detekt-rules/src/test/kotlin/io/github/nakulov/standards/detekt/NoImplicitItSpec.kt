package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class NoImplicitItSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> case.arg2 },
        case(arg1 = makeImplicitItFindings(), arg2 = IMPLICIT_IT_CODE),
        case(arg1 = makeNestedItFindings(), arg2 = NESTED_IT_CODE),
        case(arg1 = emptyList(), arg2 = NAMED_PARAMETER_CODE),
    ) { expected, code ->
        shouldReturnTest(expected) { NoImplicitIt(Config.empty).findLintLinesInFile(code, filename = "Sample.kt") }
    }
})

package io.github.nakulov.standards.gradle

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class FindMissingSetupSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> "kotlin: ${case.arg2}, порог: ${case.arg3}" },
        case(arg1 = NO_KOTLIN_ERROR, arg2 = false, arg3 = COVERAGE_MIN_BOUND),
        case(arg1 = NO_BOUND_ERROR, arg2 = true, arg3 = null),
        case(arg1 = null, arg2 = true, arg3 = COVERAGE_MIN_BOUND),
    ) { expected, hasKotlin, coverageMinBound ->
        shouldReturnTest(expected) { findMissingSetup(hasKotlin, coverageMinBound) }
    }
})

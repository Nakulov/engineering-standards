package io.github.nakulov.standards.gradle

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class StandardsKotlinPluginSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> "образец: ${case.arg2}" },
        case(arg1 = makeViolationFindings(), arg2 = "violation", arg3 = ".kt:"),
    ) { expected, fixture, mark ->
        shouldReturnTest(expected) { findFailedCheckLines(fixture, mark) }
    }
})

package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class NamedLiteralArgumentSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> case.arg2 },
        case(arg1 = makeUnnamedLiteralFindings(), arg2 = UNNAMED_LITERAL_CODE),
        case(arg1 = makeJavaLiteralFindings(), arg2 = JAVA_LITERAL_CODE),
        case(arg1 = emptyList(), arg2 = ALLOWED_LITERAL_CODE),
    ) { expected, code ->
        shouldReturnTest(expected) { NamedLiteralArgument(Config.empty).findLintLines(code) }
    }
})

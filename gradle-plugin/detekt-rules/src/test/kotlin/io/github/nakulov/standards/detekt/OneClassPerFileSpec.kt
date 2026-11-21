package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class OneClassPerFileSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> case.arg2 },
        case(arg1 = makeTwoClassesFindings(), arg2 = TWO_CLASSES_CODE),
        case(arg1 = makeSealedBesideFindings(), arg2 = SEALED_BESIDE_CODE),
        case(arg1 = emptyList(), arg2 = NESTED_CLASSES_CODE),
    ) { expected, code ->
        shouldReturnTest(expected) { OneClassPerFile(Config.empty).findLintLinesInFile(code, filename = "Sample.kt") }
    }
})

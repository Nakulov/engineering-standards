package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class SpecDataContentSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> "файл: ${case.arg2}" },
        case(arg1 = makeBrokenSpecDataFindings(), arg2 = "BrokenSpecData.kt", arg3 = BROKEN_SPEC_DATA_CODE),
        case(arg1 = emptyList(), arg2 = "ExampleSpecData.kt", arg3 = VALID_SPEC_DATA_CODE),
        case(arg1 = emptyList(), arg2 = "Example.kt", arg3 = PLAIN_FILE_CODE),
    ) { expected, filename, code ->
        shouldReturnTest(expected) { SpecDataContent(Config.empty).findLintLinesInFile(code, filename) }
    }
})

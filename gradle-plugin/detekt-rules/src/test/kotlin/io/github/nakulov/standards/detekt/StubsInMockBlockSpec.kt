package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class StubsInMockBlockSpec : BehaviorSpec({
    withGivens(
        nameFn = { case -> case.arg2 },
        case(arg1 = makeTopLevelStubFindings(), arg2 = TOP_LEVEL_EVERY_CODE),
        case(arg1 = makeTopLevelStubFindings(), arg2 = TOP_LEVEL_JUST_RUN_CODE),
        case(arg1 = makeTopLevelStubFindings(), arg2 = TOP_LEVEL_CO_EVERY_CODE),
        case(arg1 = makeTopLevelStubFindings(), arg2 = FACTORY_MOCK_STUB_CODE),
        case(arg1 = emptyList(), arg2 = MOCK_BLOCK_STUBS_CODE),
        case(arg1 = emptyList(), arg2 = NESTED_STUBS_CODE),
        case(arg1 = emptyList(), arg2 = TOP_LEVEL_VERIFY_CODE),
        case(arg1 = emptyList(), arg2 = FUN_SPEC_STUB_CODE),
        case(arg1 = emptyList(), arg2 = NO_LAMBDA_SPEC_CODE),
    ) { expected, code ->
        shouldReturnTest(expected) {
            StubsInMockBlock(Config.empty).findLintLinesInFile(code, filename = "ExampleSpec.kt")
        }
    }
})

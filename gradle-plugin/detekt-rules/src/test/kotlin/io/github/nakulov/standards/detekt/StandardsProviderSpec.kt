package io.github.nakulov.standards.detekt

import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

class StandardsProviderSpec : BehaviorSpec({
    shouldReturnTest(makeStandardsRuleSetId()) { StandardsProvider().ruleSetId }

    shouldReturnTest(makeStandardsRuleNames()) { StandardsProvider().instance().rules.keys }
})

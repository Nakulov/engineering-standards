package io.github.nakulov.standards.detekt

import dev.detekt.api.RuleName
import dev.detekt.api.RuleSetId

fun makeStandardsRuleSetId(): RuleSetId {
    return RuleSetId(value = "standards")
}

fun makeStandardsRuleNames(): Set<RuleName> {
    return setOf(
        RuleName(value = "BehaviorSpecOnly"),
        RuleName(value = "NamedLiteralArgument"),
        RuleName(value = "NoImplicitIt"),
        RuleName(value = "OneClassPerFile"),
        RuleName(value = "SpecDataContent"),
    )
}

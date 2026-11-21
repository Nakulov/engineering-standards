package io.github.nakulov.standards.detekt

import dev.detekt.api.RuleName
import dev.detekt.api.RuleSetId

/** Набор правил в `detekt.yml`: `standards`. */
fun makeStandardsRuleSetId(): RuleSetId {
    return RuleSetId(value = "standards")
}

/** Правила набора — по имени класса, как они включаются в `detekt.yml`. */
fun makeStandardsRuleNames(): Set<RuleName> {
    return setOf(
    )
}

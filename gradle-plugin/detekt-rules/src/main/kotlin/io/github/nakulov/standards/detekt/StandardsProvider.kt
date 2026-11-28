package io.github.nakulov.standards.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/** Свои правила detekt стандарта: проверки правил кодстайла и тестов, которых нет в стандартном наборе detekt. */
class StandardsProvider : RuleSetProvider {

    override val ruleSetId: RuleSetId = RuleSetId(value = "standards")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                { config -> BehaviorSpecOnly(config) },
                { config -> NamedLiteralArgument(config) },
                { config -> NoImplicitIt(config) },
                { config -> OneClassPerFile(config) },
                { config -> SpecDataContent(config) },
                { config -> StubsInMockBlock(config) },
            ),
        )
    }
}

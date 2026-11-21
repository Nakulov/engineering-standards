package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass

private const val RULE_ID = "KT-KIT-1"
private const val BEHAVIOR_SPEC = "BehaviorSpec"

internal class BehaviorSpecOnly(
    config: Config,
) : Rule(
    config,
    description = "$RULE_ID. Спеки — только $BEHAVIOR_SPEC.",
) {

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        val spec = klass.findSpecSuperType() ?: return
        if (spec == BEHAVIOR_SPEC) return
        val entity = Entity.atName(klass)
        val message = "$RULE_ID. «${klass.name}» наследует $spec: спека — только $BEHAVIOR_SPEC."
        val finding = Finding(entity, message)
        report(finding)
    }
}

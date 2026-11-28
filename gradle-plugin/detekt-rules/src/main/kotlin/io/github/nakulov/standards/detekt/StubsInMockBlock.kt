package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry

private const val RULE_ID = "KT-KIT-13"
private const val BEHAVIOR_SPEC = "BehaviorSpec"
private const val MESSAGE =
    "$RULE_ID. Заглушка верхнего уровня спеки — в блоке при создании мока `mockk<T> { … }`."
private val STUB_CALLS = setOf("every", "coEvery", "justRun", "coJustRun")

internal class StubsInMockBlock(
    config: Config,
) : Rule(
    config,
    description = "$RULE_ID. Заглушки мока верхнего уровня спеки — в блоке при создании мока.",
) {

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (klass.findSpecSuperType() != BEHAVIOR_SPEC) return
        findSpecStatements(klass)
            .mapNotNull { statement -> findLeftmostCall(statement) }
            .filter { call -> isStubCall(call) }
            .forEach { call -> reportStub(call) }
    }

    // Только прямые выражения лямбды `BehaviorSpec({ … })`: заглушки во вложенных блоках разрешены.
    private fun findSpecStatements(klass: KtClass): List<KtExpression> {
        return klass.superTypeListEntries
            .filterIsInstance<KtSuperTypeCallEntry>()
            .flatMap { entry -> entry.valueArguments }
            .mapNotNull { argument -> argument.getArgumentExpression() as? KtLambdaExpression }
            .flatMap { lambda ->
                lambda.bodyExpression
                    ?.statements
                    .orEmpty()
            }
    }

    // Инфиксная цепочка `every { … } returns … andThen …` — дерево бинарных выражений, заглушка в самом левом.
    private fun findLeftmostCall(expression: KtExpression?): KtCallExpression? {
        if (expression is KtBinaryExpression) return findLeftmostCall(expression.left)
        return expression as? KtCallExpression
    }

    private fun isStubCall(call: KtCallExpression): Boolean {
        val name = call.calleeExpression
            ?.text
        return name in STUB_CALLS
    }

    private fun reportStub(call: KtCallExpression) {
        val entity = Entity.from(call)
        val finding = Finding(entity, MESSAGE)
        report(finding)
    }
}

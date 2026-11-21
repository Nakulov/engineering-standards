package io.github.nakulov.standards.detekt

import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

private const val RULE_ID = "KT-FUN-6"
private const val IMPLICIT_IT = "it"
private const val MESSAGE = "$RULE_ID. Лямбда с неявным it: назовите параметр (`{ item -> … }`)."

internal class NoImplicitIt(
    config: Config,
) : Rule(
    config,
    description = "$RULE_ID. Параметр лямбды — с именем, неявный it не пишется.",
) {

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        super.visitLambdaExpression(lambdaExpression)
        val hasParameters = lambdaExpression.functionLiteral
            .hasParameterSpecification()
        if (hasParameters) return
        val references = PsiTreeUtil.collectElementsOfType(lambdaExpression, KtNameReferenceExpression::class.java)
        val hasImplicitIt = references.any { reference -> isImplicitItOf(reference, lambdaExpression) }
        if (!hasImplicitIt) return
        val entity = Entity.from(lambdaExpression)
        val finding = Finding(entity, MESSAGE)
        report(finding)
    }

    private fun isImplicitItOf(reference: KtNameReferenceExpression, lambda: KtLambdaExpression): Boolean {
        val owner = PsiTreeUtil.getParentOfType(reference, KtLambdaExpression::class.java)
        return reference.getReferencedName() == IMPLICIT_IT && owner == lambda
    }
}

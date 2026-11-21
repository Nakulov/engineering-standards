package io.github.nakulov.standards.detekt

import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtValueArgument

private const val RULE_ID = "KT-FUN-4"

private val NAME_COMMENT = Regex(pattern = """/\*\s*\w+\s*=\s*\*/""")

internal class NamedLiteralArgument(
    config: Config,
) : Rule(
    config = config,
    description = "$RULE_ID. Литерал в аргументе вызова — с именем параметра.",
),
    RequiresAnalysisApi {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        reportUnnamedLiterals(call = expression)
    }

    override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
        super.visitSuperTypeCallEntry(call)
        reportUnnamedLiterals(call = call)
    }

    private fun reportUnnamedLiterals(call: KtCallElement) {
        val literals = call.valueArguments.filterIsInstance<KtValueArgument>()
            .filter { argument -> isUnnamedLiteral(argument = argument) }
        if (literals.isEmpty()) {
            return
        }
        analyze(call) {
            val functionCall = call.resolveToCall()
                ?.singleFunctionCallOrNull() ?: return@analyze
            // У метода Java имена параметров не стабильны: вместо имени — комментарий /* имя = */.
            val hasStableNames = functionCall.symbol.hasStableParameterNames
            val nonVarargLiterals = literals.filter { argument ->
                functionCall.valueArgumentMapping[argument.getArgumentExpression()]?.symbol?.isVararg == false
            }
            nonVarargLiterals.forEach { argument ->
                reportArgument(argument = argument, hasStableNames = hasStableNames)
            }
        }
    }

    private fun reportArgument(argument: KtValueArgument, hasStableNames: Boolean) {
        if (!hasStableNames && hasNameComment(argument = argument)) {
            return
        }
        val hint = if (hasStableNames) {
            "назовите параметр"
        } else {
            "добавьте комментарий /* имя = */ (метод Java)"
        }
        report(
            Finding(
                entity = Entity.from(argument),
                message = "$RULE_ID. Литерал ${argument.text} без имени: $hint.",
            ),
        )
    }

    private fun isUnnamedLiteral(argument: KtValueArgument): Boolean {
        return !argument.isNamed() &&
            argument !is KtLambdaArgument &&
            isLiteral(expression = argument.getArgumentExpression())
    }

    private fun isLiteral(expression: KtExpression?): Boolean {
        return when (expression) {
            is KtConstantExpression, is KtStringTemplateExpression -> true
            is KtPrefixExpression -> expression.baseExpression is KtConstantExpression
            else -> false
        }
    }

    private fun hasNameComment(argument: KtValueArgument): Boolean {
        val comment = argument.firstChild as? PsiComment
            ?: PsiTreeUtil.skipWhitespacesBackward(argument) as? PsiComment
        return comment != null && NAME_COMMENT.matches(input = comment.text)
    }
}

package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

private const val SPEC_CLASS_RULE_ID = "KT-TEST-2"
private const val MAKE_RULE_ID = "KT-TEST-3"
private const val CONST_VAL_RULE_ID = "KT-TEST-5"
private const val SPEC_DATA_SUFFIX = "SpecData.kt"

internal class SpecDataContent(
    config: Config,
) : Rule(
    config,
    description = "$SPEC_CLASS_RULE_ID, $MAKE_RULE_ID, $CONST_VAL_RULE_ID. " +
        "В *$SPEC_DATA_SUFFIX нет классов-спек, функции — make…, val — const.",
) {

    private val makeName = Regex(pattern = "make[A-Z].*")

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        val isSpecData = file.name
            .endsWith(SPEC_DATA_SUFFIX)
        if (!isSpecData) return
        file.declarations
            .filterIsInstance<KtNamedDeclaration>()
            .forEach { declaration -> reportProblem(declaration) }
    }

    private fun reportProblem(declaration: KtNamedDeclaration) {
        val problem = findProblem(declaration) ?: return
        val entity = Entity.atName(declaration)
        val finding = Finding(entity, problem)
        report(finding)
    }

    private fun findProblem(declaration: KtNamedDeclaration): String? {
        return when (declaration) {
            is KtClass -> findSpecClassProblem(declaration)
            is KtNamedFunction -> findFunctionProblem(declaration)
            is KtProperty -> findPropertyProblem(declaration)
            else -> null
        }
    }

    private fun findSpecClassProblem(klass: KtClass): String? {
        val spec = klass.findSpecSuperType() ?: return null
        return "$SPEC_CLASS_RULE_ID. «${klass.name}»: класс-спека ($spec) в файле данных."
    }

    private fun findFunctionProblem(function: KtNamedFunction): String? {
        val name = function.name
            .orEmpty()
        if (makeName.matches(name)) return null
        return "$MAKE_RULE_ID. «${function.name}»: функция без префикса make."
    }

    private fun findPropertyProblem(property: KtProperty): String? {
        if (property.hasModifier(KtTokens.CONST_KEYWORD)) return null
        return "$CONST_VAL_RULE_ID. «${property.name}»: val без const."
    }
}

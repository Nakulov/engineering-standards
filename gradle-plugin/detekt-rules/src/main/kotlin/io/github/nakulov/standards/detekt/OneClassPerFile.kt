package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

private const val RULE_ID = "KT-NAME-3"

internal class OneClassPerFile(
    config: Config,
) : Rule(
    config,
    description = "$RULE_ID. Одна верхнеуровневая декларация на файл: второй класс — в свой файл.",
) {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        file.declarations
            .filterIsInstance<KtClassOrObject>()
            .drop(n = 1)
            .forEach { declaration -> reportSecond(declaration) }
    }

    private fun reportSecond(declaration: KtClassOrObject) {
        val entity = Entity.atName(declaration)
        val message = "$RULE_ID. «${declaration.name}» — второй класс верхнего уровня в файле: вынесите в свой файл."
        val finding = Finding(entity, message)
        report(finding)
    }
}

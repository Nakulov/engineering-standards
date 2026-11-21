package io.github.nakulov.standards.detekt

import org.jetbrains.kotlin.psi.KtClass

// Короткое имя супертипа: спеку пишут и с полным именем (`io.kotest…BehaviorSpec`), и с параметрами типа.
internal fun KtClass.findSpecSuperType(): String? {
    return superTypeListEntries
        .map { entry ->
            entry.typeReference
                ?.text
                .orEmpty()
        }
        .map { name ->
            name.substringBefore(delimiter = '<')
                .substringAfterLast(delimiter = '.')
        }
        .firstOrNull { name -> name.endsWith(suffix = "Spec") }
}

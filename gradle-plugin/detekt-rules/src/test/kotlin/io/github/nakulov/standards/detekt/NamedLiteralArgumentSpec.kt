package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

class NamedLiteralArgumentSpec : BehaviorSpec({
    shouldReturnTest(makeUnnamedLiteralFindings()) {
        NamedLiteralArgument(Config.empty).findLintLines(makeUnnamedLiteralCode())
    }

    shouldReturnTest(makeJavaLiteralFindings()) {
        NamedLiteralArgument(Config.empty).findLintLines(makeJavaLiteralCode())
    }

    shouldReturnTest(emptyList<String>()) {
        NamedLiteralArgument(Config.empty).findLintLines(makeAllowedLiteralCode())
    }
})

package io.github.nakulov.standards.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class NamedLiteralArgumentSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { case -> "код: ${case.arg2}" },
            case(arg1 = makeUnnamedLiteralFindings(), arg2 = "литералы без имени", arg3 = makeUnnamedLiteralCode()),
            case(arg1 = makeJavaLiteralFindings(), arg2 = "метод Java", arg3 = makeJavaLiteralCode()),
            case(arg1 = emptyList(), arg2 = "литералы с именем", arg3 = makeNamedLiteralCode()),
            case(arg1 = emptyList(), arg2 = "vararg, инфикс, аннотация", arg3 = makeLiteralExceptionsCode()),
        ) { expected, _, code ->
            shouldReturnTest(expected) { NamedLiteralArgument(config = Config.empty).lintCodeWithContext(code = code) }
        }
    })

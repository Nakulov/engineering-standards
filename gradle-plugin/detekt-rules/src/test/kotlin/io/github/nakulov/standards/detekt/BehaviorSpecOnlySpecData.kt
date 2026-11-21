package io.github.nakulov.standards.detekt

const val FUN_SPEC_CODE = """
class ExampleSpec : FunSpec({
})
"""

const val BEHAVIOR_SPEC_CODE = """
class ExampleSpec : io.kotest.core.spec.style.BehaviorSpec({
})

class Child : Base()
"""

fun makeFunSpecFindings(): List<String> {
    return listOf(
        "2:7 KT-KIT-1. «ExampleSpec» наследует FunSpec: спека — только BehaviorSpec.",
    )
}

package io.github.nakulov.standards.detekt

const val TWO_CLASSES_CODE = """
package sample

class First

class Second

interface Third
"""

const val SEALED_BESIDE_CODE = """
sealed interface State

data object Idle : State
"""

const val NESTED_CLASSES_CODE = """
package sample

sealed class Shape {

    data class Circle(
        val radius: Int,
    ) : Shape()

    private class Helper
}

fun makeShape(): Shape {
    return Shape.Circle(radius = 1)
}
"""

fun makeTwoClassesFindings(): List<String> {
    return listOf(
        "6:7 KT-NAME-3. «Second» — второй класс верхнего уровня в файле: вынесите в свой файл.",
        "8:11 KT-NAME-3. «Third» — второй класс верхнего уровня в файле: вынесите в свой файл.",
    )
}

fun makeSealedBesideFindings(): List<String> {
    return listOf(
        "4:13 KT-NAME-3. «Idle» — второй класс верхнего уровня в файле: вынесите в свой файл.",
    )
}

package io.github.nakulov.standards.detekt

const val IMPLICIT_IT_CODE = """
fun upper(names: List<String>): List<String> {
    return names.map { it.uppercase() }
}
"""

const val NESTED_IT_CODE = """
fun letters(names: List<String>): List<String> {
    return names.map { name -> name.filter { it.isLetter() } }
}
"""

const val NAMED_PARAMETER_CODE = """
fun upper(names: List<String>): List<String> {
    val size by lazy { names.size }
    return names.map { name -> name.uppercase() }
}
"""

fun makeImplicitItFindings(): List<String> {
    return listOf(
        "3:22 KT-FUN-6. Лямбда с неявным it: назовите параметр (`{ item -> … }`).",
    )
}

fun makeNestedItFindings(): List<String> {
    return listOf(
        "3:44 KT-FUN-6. Лямбда с неявным it: назовите параметр (`{ item -> … }`).",
    )
}

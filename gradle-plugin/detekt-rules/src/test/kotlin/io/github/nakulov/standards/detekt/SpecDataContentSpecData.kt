package io.github.nakulov.standards.detekt

const val BROKEN_SPEC_DATA_CODE = """
package sample

const val NAME = "x"

val shared = listOf(1)

fun id(): String {
    return NAME
}

fun makeup(): String {
    return NAME
}

class ExampleSpec : BehaviorSpec({
})
"""

const val VALID_SPEC_DATA_CODE = """
package sample

const val NAME = "x"

typealias Name = String

data class Fixture(
    val name: Name,
)

fun makeId(): String {
    return NAME
}

private fun makeHelper(): String {
    return NAME
}
"""

const val PLAIN_FILE_CODE = """
package sample

val shared = listOf(1)

fun id(): String {
    return "x"
}
"""

fun makeBrokenSpecDataFindings(): List<String> {
    return listOf(
        "6:5 KT-TEST-5. «shared»: val без const.",
        "8:5 KT-TEST-3. «id»: функция без префикса make.",
        "12:5 KT-TEST-3. «makeup»: функция без префикса make.",
        "16:7 KT-TEST-2. «ExampleSpec»: класс-спека (BehaviorSpec) в файле данных.",
    )
}

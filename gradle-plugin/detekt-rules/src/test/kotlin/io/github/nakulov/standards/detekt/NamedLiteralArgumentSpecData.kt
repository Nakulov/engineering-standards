package io.github.nakulov.standards.detekt

const val UNNAMED_LITERAL_CODE = """
open class Base(
    val name: String,
)

class Child : Base("x")

fun greet(name: String, times: Int, loud: Boolean, mark: Char, extra: String?) {
    println(name)
}

fun main() {
    greet("Ann", 2, true, '!', null)
    repeat(-1) { }
}
"""

const val JAVA_LITERAL_CODE = """
fun main() {
    System.err.println("x")
    System.err.println(/* p0 = */ "y")
}
"""

const val ALLOWED_LITERAL_CODE = """
fun greet(name: String, times: Int) {
    println(message = name)
}

@Deprecated("старое имя")
fun main() {
    greet(name = "Ann", times = -1)
    val list = listOf("a", "b")
    val pair = "a" to 1
    println(message = list + pair.first)
}
"""

fun makeUnnamedLiteralFindings(): List<String> {
    return listOf(
        "6:20 KT-FUN-4. Литерал \"x\" без имени: назовите параметр.",
        "13:11 KT-FUN-4. Литерал \"Ann\" без имени: назовите параметр.",
        "13:18 KT-FUN-4. Литерал 2 без имени: назовите параметр.",
        "13:21 KT-FUN-4. Литерал true без имени: назовите параметр.",
        "13:27 KT-FUN-4. Литерал '!' без имени: назовите параметр.",
        "13:32 KT-FUN-4. Литерал null без имени: назовите параметр.",
        "14:12 KT-FUN-4. Литерал -1 без имени: назовите параметр.",
    )
}

fun makeJavaLiteralFindings(): List<String> {
    return listOf(
        "3:24 KT-FUN-4. Литерал \"x\" без имени: добавьте комментарий /* имя = */ (метод Java).",
    )
}

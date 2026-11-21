package io.github.nakulov.standards.detekt

fun makeUnnamedLiteralCode(): String {
    return """
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
}

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

fun makeJavaLiteralCode(): String {
    return """
fun main() {
    System.err.println("x")
    System.err.println(/* p0 = */ "y")
}
"""
}

fun makeJavaLiteralFindings(): List<String> {
    return listOf(
        "3:24 KT-FUN-4. Литерал \"x\" без имени: добавьте комментарий /* имя = */ (метод Java).",
    )
}

fun makeNamedLiteralCode(): String {
    return """
fun greet(name: String, times: Int) {
    println(message = name)
}

fun main() {
    greet(name = "Ann", times = -1)
}
"""
}

fun makeLiteralExceptionsCode(): String {
    return """
@Suppress("UNUSED")
fun main() {
    val list = listOf("a", "b")
    val pair = "a" to 1
    println(message = list + pair.first)
}
"""
}
